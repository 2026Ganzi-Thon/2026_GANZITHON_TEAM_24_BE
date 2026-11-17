package com.ganzithon.toro.domain.stt.controller;

import com.ganzithon.toro.domain.stt.client.GroqSttClient;
import com.ganzithon.toro.domain.stt.dto.Transcription;
import com.ganzithon.toro.domain.vent.dto.FlowDtos.Stage;
import com.ganzithon.toro.domain.vent.service.VentSessionStore;
import com.ganzithon.toro.global.response.ApiResponse;
import com.ganzithon.toro.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "6. 음성 인식", description = "Groq Whisper 한국어 STT. 인식 후 사용자가 수정한 텍스트로 공감·감정 API를 호출")
@RestController
@RequestMapping("/api/v1/stt")
@RequiredArgsConstructor
public class SttController {
    private final GroqSttClient groq;
    private final VentSessionStore sessions;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "최대 60초 음성 → 텍스트", description = "본인 세션 VENTING 단계만 허용. file과 sessionId 필수. 최대 10MiB, webm/wav/mp3/mp4/m4a/ogg/flac. 길이는 외부 변환 후 검증하므로 프론트도 60초에 녹음을 중지해야 한다")
    public ResponseEntity<ApiResponse<Transcription>> transcribe(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
            @RequestParam UUID sessionId, @RequestPart("file") MultipartFile file) {
        var result = sessions.perform(user.id(), sessionId, Set.of(Stage.VENTING), view -> groq.transcribe(file));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(result));
    }
}
