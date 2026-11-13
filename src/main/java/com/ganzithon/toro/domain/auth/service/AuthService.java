package com.ganzithon.toro.domain.auth.service;

import com.ganzithon.toro.domain.auth.dto.AvailabilityResponse;
import com.ganzithon.toro.domain.auth.dto.LoginRequest;
import com.ganzithon.toro.domain.auth.dto.LoginResponse;
import com.ganzithon.toro.domain.auth.dto.SignupRequest;
import com.ganzithon.toro.domain.auth.dto.SignupResponse;
import com.ganzithon.toro.domain.character.repository.CharacterRepository;
import com.ganzithon.toro.domain.user.UserRules;
import com.ganzithon.toro.domain.user.entity.User;
import com.ganzithon.toro.domain.user.repository.UserRepository;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import com.ganzithon.toro.global.security.JwtProvider;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final String termsVersion;

    public AuthService(UserRepository userRepository, CharacterRepository characterRepository,
                       PasswordEncoder passwordEncoder, JwtProvider jwtProvider,
                       @Value("${app.terms-version}") String termsVersion) {
        this.userRepository = userRepository;
        this.characterRepository = characterRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.termsVersion = termsVersion;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        }
        User user = User.signUp(request.email(), passwordEncoder.encode(request.password()), request.nickname(),
                termsVersion, LocalDateTime.now());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw UserRules.duplicateError(e);
        }
        return new SignupResponse(user.getId());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 13자를 넘는 비밀번호는 가입될 수 없으니 바로 실패 (BCrypt 72바이트 제한도 피한다)
        if (request.password().length() > UserRules.PASSWORD_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        User user = userRepository.findByEmail(request.email())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        return new LoginResponse(
                jwtProvider.createToken(user.getId()),
                jwtProvider.expiresInSeconds(),
                new LoginResponse.LoginUser(user.getId(), user.getNickname()),
                characterRepository.existsByUserId(user.getId()));
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse checkEmail(String email) {
        return new AvailabilityResponse(!userRepository.existsByEmail(email));
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse checkNickname(String nickname) {
        return new AvailabilityResponse(!userRepository.existsByNickname(nickname));
    }
}
