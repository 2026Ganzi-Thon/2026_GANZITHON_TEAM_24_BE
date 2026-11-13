package com.ganzithon.toro.domain.auth.controller;

import com.ganzithon.toro.domain.auth.dto.AvailabilityResponse;
import com.ganzithon.toro.domain.auth.dto.LoginRequest;
import com.ganzithon.toro.domain.auth.dto.LoginResponse;
import com.ganzithon.toro.domain.auth.dto.SignupRequest;
import com.ganzithon.toro.domain.auth.dto.SignupResponse;
import com.ganzithon.toro.domain.auth.service.AuthService;
import com.ganzithon.toro.domain.user.UserRules;
import com.ganzithon.toro.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 토큰 없이 부르는 API (SecurityConfig의 /api/v1/auth/** 공개)
 */
@Tag(name = "1. 인증", description = "회원가입·로그인. 토큰 없이 부른다")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = """
            - 비밀번호: 영문과 숫자를 모두 포함해 8~13자. 특수문자 가능, 공백 불가. (비밀번호 확인 칸은 프론트에서만 비교)
            - 닉네임: 한글·영문·숫자 1~10자, 공백·특수문자 불가, 다른 사람과 중복 불가(대소문자 구분 없음)
            - 필수 약관 2개(agreeTerms, agreePrivacy) 모두 true. 선택 약관 1개는 미확정이라 아직 안 받음
            - 성공하면 토큰 없이 userId만 온다 → 로그인 화면으로

            에러: AUTH_001(409) 이미 가입된 이메일 · AUTH_004(409) 이미 사용 중인 닉네임 · COMMON_001(400) 형식 오류·약관 미동의
            """)
    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }

    @Operation(summary = "이메일 중복 확인", description = "쓸 수 있으면 available=true. 이메일 형식이 아니면 COMMON_001")
    @GetMapping("/email-check")
    public ApiResponse<AvailabilityResponse> checkEmail(@RequestParam @NotBlank @Email @Size(max = 255) String email) {
        return ApiResponse.ok(authService.checkEmail(email.strip()));
    }

    @Operation(summary = "로그인", description = """
            - 응답의 accessToken을 이후 모든 요청 헤더에 `Authorization: Bearer {토큰}`으로 넣는다. expiresIn은 초(7일)
            - 로그인하면 항상 홈으로. hasCharacter는 저장된 캐릭터가 1개 이상인지(홈 표시용)

            에러: AUTH_002(401) 이메일 또는 비밀번호가 틀림. (COMMON_002도 401이니 꼭 code로 구분)
            """)
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "닉네임 중복 확인", description = """
            쓸 수 있으면 available=true. 대소문자 구분 없이 비교. 닉네임 규칙(한글·영문·숫자 1~10자)에 안 맞으면 COMMON_001.
            확인 뒤 가입 전에 다른 사람이 먼저 쓰면 회원가입에서 AUTH_004가 올 수 있다.
            """)
    @GetMapping("/nickname-check")
    public ApiResponse<AvailabilityResponse> checkNickname(
            @RequestParam @Pattern(regexp = UserRules.NAME_REGEX, message = UserRules.NAME_MESSAGE) String nickname) {
        return ApiResponse.ok(authService.checkNickname(nickname));
    }
}
