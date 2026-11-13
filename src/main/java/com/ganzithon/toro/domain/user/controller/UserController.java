package com.ganzithon.toro.domain.user.controller;

import com.ganzithon.toro.domain.user.dto.ChangeEmailRequest;
import com.ganzithon.toro.domain.user.dto.ChangePasswordRequest;
import com.ganzithon.toro.domain.user.dto.EmailResponse;
import com.ganzithon.toro.domain.user.dto.MeResponse;
import com.ganzithon.toro.domain.user.dto.UpdateMeRequest;
import com.ganzithon.toro.domain.user.service.UserService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 마이페이지 */
@Tag(name = "2. 마이페이지", description = "내 정보 조회·수정, 탈퇴. 토큰 필요")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = """
            - background: 홈 화면 배경 선택지 이름(기본 DEFAULT). 배경은 사용자 전체에 1개
            - recentCharacter: 가장 최근에 사용한 캐릭터(마이페이지 프로필 이미지용). 없으면 null → 기본 말랑이 표시
            """)
    @GetMapping
    public ApiResponse<MeResponse> getMe(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(userService.getMe(user.id()));
    }

    @Operation(summary = "회원 탈퇴", description = "계정과 캐릭터를 모두 지운다. 204(본문 없음). 이후 토큰은 COMMON_002")
    @DeleteMapping
    public ResponseEntity<Void> withdraw(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user) {
        userService.withdraw(user.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "닉네임·홈 배경 수정", description = """
            보낸 필드만 바뀐다(둘 다 선택). 닉네임 규칙은 회원가입과 같다. 배경은 선택지 이름 1~20자.
            닉네임을 바꿔도 토큰은 그대로 쓰면 된다.

            에러: AUTH_004(409) 다른 사람이 쓰는 닉네임 · COMMON_001(400) 형식 오류
            """)
    @PatchMapping
    public ApiResponse<MeResponse> updateMe(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
                                            @Valid @RequestBody UpdateMeRequest request) {
        return ApiResponse.ok(userService.updateMe(user.id(), request));
    }

    @Operation(summary = "아이디(이메일) 변경", description = """
            본인 확인을 위해 현재 비밀번호가 필요하다. 토큰은 그대로 쓰면 된다.

            에러: AUTH_003(400) 현재 비밀번호가 틀림(로그아웃되지 않게 401이 아니라 400) · AUTH_001(409) 다른 사람이 쓰는 이메일
            """)
    @PatchMapping("/email")
    public ApiResponse<EmailResponse> changeEmail(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
                                                  @Valid @RequestBody ChangeEmailRequest request) {
        return ApiResponse.ok(userService.changeEmail(user.id(), request));
    }

    @Operation(summary = "비밀번호 변경", description = """
            새 비밀번호 규칙은 회원가입과 같다(영문+숫자 8~13자). 204(본문 없음). 토큰은 그대로 쓰면 된다.

            에러: AUTH_003(400) 현재 비밀번호가 틀림 · COMMON_001(400) 새 비밀번호 형식 오류
            """)
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser user,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user.id(), request);
        return ResponseEntity.noContent().build();
    }
}
