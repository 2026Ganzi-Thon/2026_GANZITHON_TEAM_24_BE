package com.ganzithon.toro.domain.vent.controller;

import com.ganzithon.toro.domain.vent.dto.EmotionSuggestionRequest;
import com.ganzithon.toro.domain.vent.dto.EmotionSuggestionResponse;
import com.ganzithon.toro.domain.vent.service.EmotionSuggestionService;
import com.ganzithon.toro.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "4. 감정 후보", description = "Upstage로 짧은 공감과 미확정 감정 후보를 제안. 로그인 필요")
@RestController
@RequestMapping("/api/v1/vent/emotions")
@RequiredArgsConstructor
public class EmotionSuggestionController {

    private final EmotionSuggestionService service;

    @Operation(summary = "감정 후보 제안 (이전 단독 API)", deprecated = true, description = """
            신규 화면은 /api/v1/vent/sessions/{id}/emotions를 사용한다. 이 단독 API에는 세션 단계·삭제 처리가 없다.
            텍스트 1~2000자 → 짧은 공감과 감정 후보 2~3개. 감정을 확정하거나 저장하지 않는다.
            프론트는 직접 입력, 아직 모르겠어요, 후보 다시 보기, 건너뛰기를 제공한다.
            다시 보기는 같은 API를 재호출하며 결과가 같을 수도 있다. 매번 API 사용량이 발생한다.
            오류: COMMON_001 입력 형식 · AI_001 키 미설정 · AI_002 외부 호출 실패 ·
            AI_003 응답 검증 실패 · AI_004 외부 요청 제한.
            """)
    @PostMapping
    public ResponseEntity<ApiResponse<EmotionSuggestionResponse>> suggest(
            @Valid @RequestBody EmotionSuggestionRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(service.suggest(request.text())));
    }
}
