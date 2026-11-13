package com.ganzithon.toro.domain.user.entity;

import com.ganzithon.toro.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname")
})
public class User extends BaseTimeEntity {

    public static final String DEFAULT_BACKGROUND = "DEFAULT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    /** BCrypt 해시 */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 10)
    private String nickname;

    /** 개인정보 수집·이용 동의 시각 (약관은 이것 1개) */
    @Column(nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime privacyAgreedAt;

    @Column(nullable = false, length = 20)
    private String termsVersion;

    /** 홈 화면 배경 */
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'DEFAULT'")
    private String background = DEFAULT_BACKGROUND;

    public static User signUp(String email, String encodedPassword, String nickname, String termsVersion,
                              LocalDateTime agreedAt) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.nickname = nickname;
        user.termsVersion = termsVersion;
        user.privacyAgreedAt = agreedAt;
        return user;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeBackground(String background) {
        this.background = background;
    }

    public void changeEmail(String email) {
        this.email = email;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
