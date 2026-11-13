package com.ganzithon.toro.domain.character.controller;

import com.ganzithon.toro.domain.character.dto.CharacterCreateRequest;
import com.ganzithon.toro.domain.character.dto.CharacterCreateResponse;
import com.ganzithon.toro.domain.character.dto.CharacterListResponse;
import com.ganzithon.toro.domain.character.dto.CharacterResponse;
import com.ganzithon.toro.domain.character.dto.CharacterUpdateRequest;
import com.ganzithon.toro.domain.character.dto.CharacterUseResponse;
import com.ganzithon.toro.domain.character.service.CharacterService;
import com.ganzithon.toro.global.response.ApiResponse;
import com.ganzithon.toro.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "3. 캐릭터", description = """
        사용자가 꾸민 캐릭터(최대 3개). 기본 캐릭터 말랑이는 서버에 없고 프론트 고정값이다. 토큰 필요.
        꾸미기 항목(color, eyes, nose, mouth, eyebrows, accessory)은 모두 1개만 고르고, 프론트 선택지 이름을 문자로 보낸다(20자 이내).
        코·눈썹이 없으면 "NONE", 장신구가 없으면 null. 선택지 목록과 3D 파일은 프론트가 가진다.
        캐릭터 이름: 한글·영문·숫자 1~10자. 연락처·이메일·@아이디·URL 모양이면 CHAR_004.
        """)
@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @Operation(summary = "내 캐릭터 목록", description = "최근 사용순(lastUsedAt). \"3일 전 사용\" 같은 표시는 lastUsedAt으로 계산")
    @GetMapping
    public ApiResponse<CharacterListResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(characterService.list(user.id()));
    }

    @Operation(summary = "캐릭터 만들기", description = """
            '이 캐릭터로 하소연 시작'을 누를 때 호출(별도 저장 버튼 없음). lastUsedAt은 지금 시각.
            이미 3개면 교체할 캐릭터를 골라 replaceId에 넣어 다시 호출한다. 3개 미만이면 replaceId는 무시.

            에러: CHAR_003(409) 이미 3개인데 replaceId 없음 · CHAR_001(404) replaceId가 내 캐릭터가 아님 ·
            CHAR_004(400) 이름에 연락처·이메일·SNS 계정 · COMMON_001(400) 형식 오류
            """)
    @PostMapping
    public ApiResponse<CharacterCreateResponse> create(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
                                                       @Valid @RequestBody CharacterCreateRequest request) {
        return ApiResponse.ok(characterService.create(user.id(), request));
    }

    @Operation(summary = "캐릭터 다시 꾸미기", description = """
            모든 항목을 보낸 값으로 바꾼다. 목록 순서(lastUsedAt)는 바뀌지 않는다.

            에러: CHAR_001(404) 없거나 내 캐릭터가 아님 · CHAR_004(400) · COMMON_001(400)
            """)
    @PutMapping("/{id}")
    public ApiResponse<CharacterResponse> update(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody CharacterUpdateRequest request) {
        return ApiResponse.ok(characterService.update(user.id(), id, request));
    }

    @Operation(summary = "캐릭터 지우기", description = "204(본문 없음). 에러: CHAR_001(404) 없거나 내 캐릭터가 아님")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
                                       @PathVariable Long id) {
        characterService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "캐릭터 사용 표시", description = """
            홈에서 저장된 캐릭터를 골라 하소연을 시작할 때 호출. lastUsedAt을 지금으로 바꿔 목록 맨 앞으로 온다.
            새로 만든 캐릭터(만들 때 이미 갱신)와 기본 말랑이는 부르지 않는다. 실패해도 하소연은 그냥 진행해도 된다.

            에러: CHAR_001(404) 없거나 내 캐릭터가 아님
            """)
    @PostMapping("/{id}/use")
    public ApiResponse<CharacterUseResponse> markUsed(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
                                                      @PathVariable Long id) {
        return ApiResponse.ok(characterService.markUsed(user.id(), id));
    }
}
