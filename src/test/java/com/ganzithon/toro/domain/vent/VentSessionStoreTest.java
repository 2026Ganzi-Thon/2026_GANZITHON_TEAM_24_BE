package com.ganzithon.toro.domain.vent;

import static com.ganzithon.toro.domain.vent.dto.FlowDtos.Stage.*;
import static org.assertj.core.api.Assertions.*;

import com.ganzithon.toro.domain.user.service.UserWithdrawnEvent;
import com.ganzithon.toro.domain.vent.dto.FlowDtos.*;
import com.ganzithon.toro.domain.vent.service.VentSessionStore;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import java.time.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class VentSessionStoreTest {
    private final MutableClock clock = new MutableClock();
    private final VentSessionStore store = new VentSessionStore(clock, Duration.ofMinutes(15), 256);

    @Test void AI없이_감정미확정_진정건너뛰기로_끝까지_진행하고_삭제한다() {
        UUID id = store.create(1).sessionId();
        assertThat(store.confirmEmotion(1, id, "").stage()).isEqualTo(CALMING);
        assertThat(store.completeCalming(1, id, true).calmingSkipped()).isTrue();
        assertThat(store.confirmReflection(1, id, reflection("")).emotion()).isEmpty();
        assertThat(store.confirmSummary(1, id, "이 사람은 약속이 지켜지길 원했다.").stage()).isEqualTo(PLAN);
        var result = store.confirmPlan(1, id, new Plan("다시 약속이 바뀌면", "가능한 시간을 물어본다"));
        assertThat(result.stage()).isEqualTo(RESULT);
        assertThat(store.get(1, id).plan()).isEqualTo(result.plan());
        var receipt = store.discard(1, id);
        assertThat(receipt.serverStateCleared()).isTrue();
        assertThat(receipt.requestsSettled()).isTrue();
        assertThat(receipt.externalDeletionVerified()).isFalse();
        error(() -> store.get(1, id), ErrorCode.SESSION_CLOSED);
        assertThat(store.discard(1, id)).isEqualTo(receipt);
    }

    @Test void 앞단계_수정시_기존_정리와_계획을_무효화한다() {
        UUID id = completed();
        var revised = store.confirmSummary(1, id, "수정한 상황 정리");
        assertThat(revised.stage()).isEqualTo(PLAN);
        assertThat(revised.plan()).isNull();
        store.confirmPlan(1, id, new Plan("새 상황", "새 행동"));
        revised = store.confirmReflection(1, id, reflection("서운함"));
        assertThat(revised.stage()).isEqualTo(SUMMARY);
        assertThat(revised.summary()).isNull();
        assertThat(revised.plan()).isNull();
    }

    @Test void 타인과_잘못된_단계는_외부호출_전에_차단한다() {
        UUID id = store.create(1).sessionId();
        error(() -> store.get(2, id), ErrorCode.SESSION_NOT_FOUND);
        error(() -> store.discard(2, id), ErrorCode.SESSION_NOT_FOUND);
        error(() -> store.perform(2, id, Set.of(VENTING), v -> { throw new AssertionError(); }), ErrorCode.SESSION_NOT_FOUND);
        error(() -> store.confirmPlan(1, id, new Plan("조건", "행동")), ErrorCode.INVALID_SESSION_STAGE);
        store.confirmEmotion(1, id, "");
        error(() -> store.perform(1, id, Set.of(VENTING), v -> { throw new AssertionError(); }), ErrorCode.INVALID_SESSION_STAGE);
    }

    @Test void 시간만료와_삭제확인_메타데이터_정리() {
        UUID id = completed();
        clock.now = clock.now.plusSeconds(900);
        store.cleanup();
        error(() -> store.get(1, id), ErrorCode.SESSION_CLOSED);
        clock.now = clock.now.plusSeconds(120);
        store.cleanup();
        error(() -> store.get(1, id), ErrorCode.SESSION_NOT_FOUND);
    }

    @Test void 처리중_삭제는_대기상태를_알리고_늦은_결과를_폐기한다() throws Exception {
        UUID id = store.create(1).sessionId();
        var started = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            Future<?> call = executor.submit(() -> store.perform(1, id, Set.of(VENTING), view -> {
                started.countDown();
                try { if (!finish.await(5, TimeUnit.SECONDS)) throw new AssertionError("timeout"); }
                catch (InterruptedException e) { throw new AssertionError(e); }
                return "늦은 원문 결과";
            }));
            try {
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                error(() -> store.confirmEmotion(1, id, "분노"), ErrorCode.INVALID_SESSION_STAGE);
                error(() -> store.perform(1, id, Set.of(VENTING), v -> "중복"), ErrorCode.INVALID_SESSION_STAGE);
                assertThat(store.discard(1, id).requestsSettled()).isFalse();
            } finally { finish.countDown(); }
            assertThatThrownBy(() -> call.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .cause().isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SESSION_CLOSED));
            assertThat(store.discard(1, id).requestsSettled()).isTrue();
        }
    }

    @Test void AI실패후_직접입력과_재시도가_가능하다() {
        UUID id = store.create(1).sessionId();
        error(() -> store.perform(1, id, Set.of(VENTING), view -> {
            throw new BusinessException(ErrorCode.AI_BUSY);
        }), ErrorCode.AI_BUSY);
        String draft = store.perform(1, id, Set.of(VENTING), view -> "초안");
        assertThat(draft).isEqualTo("초안");
        assertThat(store.get(1, id).emotion()).isEmpty();
        assertThat(store.confirmEmotion(1, id, "").stage()).isEqualTo(CALMING);
    }

    @Test void 탈퇴시_해당_사용자의_세션만_닫는다() {
        UUID own = completed(); UUID other = store.create(2).sessionId();
        store.onWithdraw(new UserWithdrawnEvent(1));
        error(() -> store.get(1, own), ErrorCode.SESSION_CLOSED);
        assertThat(store.get(2, other).stage()).isEqualTo(VENTING);
    }

    @Test void 사용자별_세션_개수와_전체_메모리_항목을_제한한다() {
        for (int i = 0; i < 3; i++) store.create(1);
        error(() -> store.create(1), ErrorCode.SESSION_LIMIT);
        var small = new VentSessionStore(clock, Duration.ofMinutes(15), 1);
        var id = small.create(1).sessionId(); small.discard(1, id);
        error(() -> small.create(2), ErrorCode.SESSION_LIMIT);
        clock.now = clock.now.plusSeconds(120);
        assertThat(small.create(2).stage()).isEqualTo(VENTING);
    }

    private UUID completed() {
        UUID id = store.create(1).sessionId();
        store.confirmEmotion(1, id, ""); store.completeCalming(1, id, false);
        store.confirmReflection(1, id, reflection("")); store.confirmSummary(1, id, "확정한 정리");
        store.confirmPlan(1, id, new Plan("조건", "행동")); return id;
    }
    private Reflection reflection(String emotion) { return new Reflection("약속이 바뀌었다", emotion, "존중", "미리 알려주기"); }
    private void error(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(code));
    }
    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-17T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
