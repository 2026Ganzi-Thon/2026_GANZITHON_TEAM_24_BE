package com.ganzithon.toro.domain.vent.service;

import static com.ganzithon.toro.domain.vent.dto.FlowDtos.Stage.*;

import com.ganzithon.toro.domain.vent.dto.FlowDtos.*;
import com.ganzithon.toro.domain.user.service.UserWithdrawnEvent;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** 단일 서버용 임시 메모리 저장소. DB/Redis/파일 직렬화를 하지 않는다. */
@Component
public class VentSessionStore {
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Clock clock;
    private final Duration ttl;
    private final int maxSessions;

    @Autowired
    public VentSessionStore(@Value("${vent.session-ttl:15m}") Duration ttl,
                            @Value("${vent.max-sessions:256}") int maxSessions) {
        this(Clock.systemUTC(), ttl, maxSessions);
    }

    public VentSessionStore(Clock clock, Duration ttl, int maxSessions) {
        if (ttl.isNegative() || ttl.isZero() || maxSessions < 1) throw new IllegalArgumentException("Invalid session settings");
        this.clock = clock; this.ttl = ttl; this.maxSessions = maxSessions;
    }

    public synchronized SessionView create(long owner) {
        cleanup();
        long active = sessions.values().stream().filter(s -> s.owner == owner && s.stage != CLOSED).count();
        if (active >= 3 || sessions.size() >= maxSessions) fail(ErrorCode.SESSION_LIMIT);
        var session = new Session(owner, clock.instant().plus(ttl));
        sessions.put(session.id, session);
        return view(session);
    }

    public synchronized SessionView get(long owner, UUID id) { return view(open(owner, id)); }

    /** 처리 중 원문은 저장소에 넣지 않는다. 단계 변경/삭제 중 늦게 도착한 AI 결과는 버린다. */
    public <T> T perform(long owner, UUID id, Set<Stage> allowed, Function<SessionView, T> operation) {
        Session session;
        SessionView input;
        long revision;
        synchronized (this) {
            session = ready(owner, id, allowed);
            session.busy = true;
            revision = session.revision;
            input = view(session);
        }
        try {
            T result = operation.apply(input);
            synchronized (this) {
                expire(session);
                if (session.stage == CLOSED) fail(ErrorCode.SESSION_CLOSED);
                if (session.revision != revision) fail(ErrorCode.INVALID_SESSION_STAGE);
                return result;
            }
        } finally {
            synchronized (this) { session.busy = false; }
        }
    }

    public synchronized SessionView confirmEmotion(long owner, UUID id, String emotion) {
        var s = ready(owner, id, Set.of(VENTING));
        s.emotion = emotion; s.stage = CALMING; s.revision++;
        return view(s);
    }

    public synchronized SessionView completeCalming(long owner, UUID id, boolean skipped) {
        var s = ready(owner, id, Set.of(CALMING));
        s.calmingSkipped = skipped; s.stage = REFLECTION; s.revision++;
        return view(s);
    }

    public synchronized SessionView confirmReflection(long owner, UUID id, Reflection reflection) {
        var s = ready(owner, id, Set.of(REFLECTION, SUMMARY, PLAN, RESULT));
        s.emotion = reflection.emotion(); s.reflection = reflection;
        s.summary = null; s.plan = null; s.stage = SUMMARY; s.revision++;
        return view(s);
    }

    public synchronized SessionView confirmSummary(long owner, UUID id, String summary) {
        var s = ready(owner, id, Set.of(SUMMARY, PLAN, RESULT));
        s.summary = summary; s.plan = null; s.stage = PLAN; s.revision++;
        return view(s);
    }

    public synchronized SessionView confirmPlan(long owner, UUID id, Plan plan) {
        var s = ready(owner, id, Set.of(PLAN, RESULT));
        s.plan = plan; s.stage = RESULT; s.revision++;
        return view(s);
    }

    /** 처리 중이면 202로 회신할 수 있게 별도 상태를 준다. 삭제 확인을 위해 내용 없는 메타데이터만 잠시 둔다. */
    public synchronized DeletionReceipt discard(long owner, UUID id) {
        var s = owned(owner, id);
        close(s);
        return new DeletionReceipt(true, !s.busy, true, false);
    }

    @TransactionalEventListener
    public synchronized void onWithdraw(UserWithdrawnEvent event) {
        sessions.values().stream().filter(s -> s.owner == event.userId()).forEach(this::close);
    }

    @Scheduled(fixedDelayString = "${vent.cleanup-interval-ms:30000}")
    public synchronized void cleanup() {
        sessions.values().forEach(this::expire);
        sessions.values().removeIf(s -> s.stage == CLOSED && !s.busy
                && !clock.instant().isBefore(s.closedAt.plusSeconds(120)));
    }

    @PreDestroy
    public synchronized void clear() {
        sessions.values().forEach(this::close);
        sessions.clear();
    }

    private Session ready(long owner, UUID id, Set<Stage> allowed) {
        var s = open(owner, id);
        if (s.busy || !allowed.contains(s.stage)) fail(ErrorCode.INVALID_SESSION_STAGE);
        return s;
    }
    private Session owned(long owner, UUID id) {
        var s = sessions.get(id);
        if (s == null || s.owner != owner) fail(ErrorCode.SESSION_NOT_FOUND);
        return s;
    }
    private Session open(long owner, UUID id) {
        var s = owned(owner, id);
        expire(s);
        if (s.stage == CLOSED) fail(ErrorCode.SESSION_CLOSED);
        return s;
    }
    private void expire(Session s) {
        if (!clock.instant().isBefore(s.expiresAt)) close(s);
    }
    private void close(Session s) {
        if (s.stage != CLOSED) {
            s.stage = CLOSED; s.closedAt = clock.instant(); s.revision++;
            s.emotion = null; s.calmingSkipped = null; s.reflection = null; s.summary = null; s.plan = null;
        }
    }
    private SessionView view(Session s) {
        return new SessionView(s.id, s.stage, s.expiresAt, s.emotion, s.calmingSkipped,
                s.reflection, s.summary, s.plan);
    }
    private static void fail(ErrorCode code) { throw new BusinessException(code); }

    private static final class Session {
        final UUID id = UUID.randomUUID();
        final long owner;
        final Instant expiresAt;
        Stage stage = VENTING;
        String emotion = "";
        Boolean calmingSkipped;
        Reflection reflection;
        String summary;
        Plan plan;
        boolean busy;
        long revision;
        Instant closedAt;
        Session(long owner, Instant expiresAt) { this.owner = owner; this.expiresAt = expiresAt; }
    }
}
