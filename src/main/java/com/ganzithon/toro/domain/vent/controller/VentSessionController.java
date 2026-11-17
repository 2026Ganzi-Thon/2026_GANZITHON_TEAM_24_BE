package com.ganzithon.toro.domain.vent.controller;

import static com.ganzithon.toro.domain.vent.dto.FlowDtos.Stage.*;

import com.ganzithon.toro.domain.vent.dto.EmotionSuggestionRequest;
import com.ganzithon.toro.domain.vent.dto.EmotionSuggestionResponse;
import com.ganzithon.toro.domain.vent.dto.FlowDtos.*;
import com.ganzithon.toro.domain.vent.service.EmotionSuggestionService;
import com.ganzithon.toro.domain.vent.service.ReflectionAiService;
import com.ganzithon.toro.domain.vent.service.VentSessionStore;
import com.ganzithon.toro.global.response.ApiResponse;
import com.ganzithon.toro.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "5. 대화 세션", description = "표현 → 감정 확인 → 진정 → 욕구 → 3인칭 정리 → 계획 → 결과 → 버리기. 확정 내용은 메모리에만 최대 15분 보관")
@RestController
@RequestMapping("/api/v1/vent/sessions")
@RequiredArgsConstructor
public class VentSessionController {
    private final VentSessionStore sessions;
    private final EmotionSuggestionService emotions;
    private final ReflectionAiService ai;

    @PostMapping
    @Operation(summary = "대화 세션 시작", description = "사용자당 최대 3개. 서버 재시작 시 소멸. 세션 ID를 클라이언트 메모리에만 보관")
    public ResponseEntity<ApiResponse<SessionView>> create(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user) {
        return ok(sessions.create(user.id()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "현재 단계와 사용자 확정 결과 조회", description = "진행 중인 본인 세션만 조회. AI 초안·하소연·음성·STT 결과는 반환하지 않는다")
    public ResponseEntity<ApiResponse<SessionView>> get(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
                                                        @PathVariable UUID id) { return ok(sessions.get(user.id(), id)); }

    @PostMapping("/{id}/messages")
    @Operation(summary = "하소연에 짧은 공감", description = "현재 메시지만 분석. 서버에 대화 이력을 만들지 않는다")
    public ResponseEntity<ApiResponse<Empathy>> message(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id, @Valid @RequestBody EmotionSuggestionRequest body) {
        return ok(sessions.perform(user.id(), id, Set.of(VENTING), view -> ai.empathize(body.text())));
    }

    @PostMapping("/{id}/emotions")
    @Operation(summary = "감정 후보 2~3개 제안·다시 보기", description = "미확정 후보는 저장하지 않는다. 입력을 합칠 경우 최대 2000자")
    public ResponseEntity<ApiResponse<EmotionSuggestionResponse>> emotions(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id, @Valid @RequestBody EmotionSuggestionRequest body) {
        return ok(sessions.perform(user.id(), id, Set.of(VENTING), view -> emotions.suggest(body.text())));
    }

    @PostMapping("/{id}/emotions/confirm")
    @Operation(summary = "감정 확인·직접 입력·모르겠어요", description = "emotion은 사용자가 고친 단어 또는 빈 값. 이 경계에서 프론트도 원문·음성·STT·공감·미선택 후보를 삭제해야 한다")
    public ResponseEntity<ApiResponse<SessionView>> emotion(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id, @Valid @RequestBody EmotionConfirmation body) {
        return ok(sessions.confirmEmotion(user.id(), id, body.emotion()));
    }

    @GetMapping("/{id}/calming")
    @Operation(summary = "짧은 진정 안내", description = "타이머는 프론트에서 표시. 건너뛰기 가능, AI 호출 없음")
    public ResponseEntity<ApiResponse<CalmingGuide>> calming(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
                                                             @PathVariable UUID id) {
        return ok(sessions.perform(user.id(), id, Set.of(CALMING), view -> new CalmingGuide(20,
                List.of("잠시 화면에서 눈을 떼어도 좋아요.", "편한 자세로 주변을 천천히 둘러보세요.", "준비되면 다음으로 넘어가세요."), true)));
    }

    @PostMapping("/{id}/calming/complete")
    @Operation(summary = "진정 완료 또는 건너뛰기", description = "skipped=true면 건너뛰기")
    public ResponseEntity<ApiResponse<SessionView>> calm(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id, @Valid @RequestBody CalmCompletion body) {
        return ok(sessions.completeCalming(user.id(), id, body.skipped()));
    }

    @PostMapping("/{id}/needs/suggestions")
    @Operation(summary = "욕구와 원했던 반응 초안", description = "원래 하소연을 재전송하지 않는다. 사용자가 이 단계에서 새로 적은 짧은 상황과 원했던 반응을 사용한다. AI 실패 시 직접 확정 가능")
    public ResponseEntity<ApiResponse<NeedsSuggestion>> needs(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id, @Valid @RequestBody NeedsInput body) {
        return ok(sessions.perform(user.id(), id, Set.of(REFLECTION, SUMMARY, PLAN, RESULT),
                view -> ai.needs(view.emotion(), body)));
    }

    @PutMapping("/{id}/reflection")
    @Operation(summary = "사용자가 상황·감정·욕구·원했던 반응 확정", description = "감정·욕구·반응은 아직 모르면 빈 값 허용. 수정 시 이전 요약과 계획은 제거되고 다시 확인해야 한다")
    public ResponseEntity<ApiResponse<SessionView>> reflection(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id, @Valid @RequestBody Reflection body) {
        return ok(sessions.confirmReflection(user.id(), id, body));
    }

    @PostMapping("/{id}/summary/suggestions")
    @Operation(summary = "3인칭 상황 정리 초안", description = "확정된 reflection만 사용. 생성만으로 단계가 넘어가지 않는다")
    public ResponseEntity<ApiResponse<SummarySuggestion>> summarySuggestion(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
                                                                           @PathVariable UUID id) {
        return ok(sessions.perform(user.id(), id, Set.of(SUMMARY, PLAN, RESULT), view -> ai.summarize(view.reflection())));
    }

    @PutMapping("/{id}/summary")
    @Operation(summary = "사용자가 고친 3인칭 정리 확정", description = "AI 제안을 쓰지 않고 직접 작성해도 된다. 수정 시 이전 계획은 제거된다")
    public ResponseEntity<ApiResponse<SessionView>> summary(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id, @Valid @RequestBody SummaryConfirmation body) {
        return ok(sessions.confirmSummary(user.id(), id, body.summary()));
    }

    @PostMapping("/{id}/plan/suggestions")
    @Operation(summary = "만약–그러면 계획 초안", description = "사용자가 확정한 요약을 기반으로 작은 행동을 제안한다. 실제 메시지를 보내지 않는다")
    public ResponseEntity<ApiResponse<PlanSuggestion>> planSuggestion(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
                                                                      @PathVariable UUID id) {
        return ok(sessions.perform(user.id(), id, Set.of(PLAN, RESULT), view -> ai.plan(view.summary(), view.reflection())));
    }

    @PutMapping("/{id}/plan")
    @Operation(summary = "사용자 계획 확정·수정", description = "확정 후 RESULT 단계. GET 세션으로 확인하고 각 PUT으로 수정 가능")
    public ResponseEntity<ApiResponse<SessionView>> plan(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id, @Valid @RequestBody Plan body) {
        return ok(sessions.confirmPlan(user.id(), id, body));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "감정 버리기·서버 임시 상태 삭제 확인", description = "확정 내용을 즉시 제거. 진행 중 요청이 남으면 202, 없으면 200. 202면 같은 DELETE를 다시 호출. 프론트 메모리 삭제와 외부 제공자의 삭제 확인은 별도이며 이 응답으로 보증하지 않는다")
    public ResponseEntity<ApiResponse<DeletionReceipt>> discard(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
                                                                 @PathVariable UUID id) {
        var receipt = sessions.discard(user.id(), id);
        return ResponseEntity.status(receipt.requestsSettled() ? 200 : 202)
                .cacheControl(CacheControl.noStore()).body(ApiResponse.ok(receipt));
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(T value) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(value));
    }
}
