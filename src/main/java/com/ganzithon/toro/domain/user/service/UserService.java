package com.ganzithon.toro.domain.user.service;

import com.ganzithon.toro.domain.character.repository.CharacterRepository;
import com.ganzithon.toro.domain.user.UserRules;
import com.ganzithon.toro.domain.user.dto.ChangeEmailRequest;
import com.ganzithon.toro.domain.user.dto.ChangePasswordRequest;
import com.ganzithon.toro.domain.user.dto.EmailResponse;
import com.ganzithon.toro.domain.user.dto.MeResponse;
import com.ganzithon.toro.domain.user.dto.UpdateMeRequest;
import com.ganzithon.toro.domain.user.entity.User;
import com.ganzithon.toro.domain.user.repository.UserRepository;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 다른 도메인(하소연 3인칭 문장 등)에서 닉네임이 필요할 때 쓴다.
     * 닉네임은 마이페이지에서 바뀔 수 있어서 토큰이 아니라 DB에서 읽는다.
     */
    @Transactional(readOnly = true)
    public String getNickname(Long userId) {
        return getUser(userId).getNickname();
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        return MeResponse.from(getUser(userId));
    }

    @Transactional
    public MeResponse updateMe(Long userId, UpdateMeRequest request) {
        User user = getUser(userId);
        String nickname = request.nickname();
        if (nickname != null && !nickname.equalsIgnoreCase(user.getNickname())) {
            if (userRepository.existsByNickname(nickname)) {
                throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
            }
        }
        if (nickname != null) {
            user.changeNickname(nickname);
        }
        if (request.background() != null) {
            user.changeBackground(request.background());
        }
        flushOrDuplicate();
        return MeResponse.from(user);
    }

    @Transactional
    public EmailResponse changeEmail(Long userId, ChangeEmailRequest request) {
        User user = getUser(userId);
        checkCurrentPassword(user, request.currentPassword());
        String email = request.email();
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        user.changeEmail(email);
        flushOrDuplicate();
        return new EmailResponse(user.getEmail());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUser(userId);
        checkCurrentPassword(user, request.currentPassword());
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    /** 탈퇴: 캐릭터를 먼저 지우고 계정을 지운다 (DB의 ON DELETE CASCADE가 없어도 남지 않게) */
    @Transactional
    public void withdraw(Long userId) {
        User user = getUser(userId);
        characterRepository.deleteAllByUserId(userId);
        userRepository.delete(user);
    }

    private User getUser(Long userId) {
        // JWT 필터가 탈퇴한 사용자를 걸러 주지만, 그 사이에 지워졌으면 다시 로그인하게 한다
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /** 틀리면 AUTH_003(400). 401로 보내면 프론트가 로그아웃시키기 때문 */
    private void checkCurrentPassword(User user, String currentPassword) {
        if (currentPassword.length() > UserRules.PASSWORD_MAX_LENGTH
                || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.WRONG_CURRENT_PASSWORD);
        }
    }

    private void flushOrDuplicate() {
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw UserRules.duplicateError(e);
        }
    }
}
