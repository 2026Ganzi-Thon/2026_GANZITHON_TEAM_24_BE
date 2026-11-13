package com.ganzithon.toro.domain.character.entity;

import com.ganzithon.toro.domain.user.entity.User;
import com.ganzithon.toro.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 사용자가 꾸민 캐릭터. java.lang.Character와 헷갈리지 않게 UserCharacter로 이름 지었다.
 * 꾸미기 값은 프론트 선택지 이름을 문자 그대로 저장한다(선택지 목록과 3D 파일은 프론트가 가진다).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "characters", indexes = @Index(name = "idx_characters_user_last_used",
        columnList = "user_id, last_used_at"))
public class UserCharacter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_characters_user"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(nullable = false, length = 20)
    private String color;

    @Column(nullable = false, length = 20)
    private String eyes;

    @Column(nullable = false, length = 20)
    private String nose;

    @Column(nullable = false, length = 20)
    private String mouth;

    @Column(nullable = false, length = 20)
    private String eyebrows;

    /** 안 고르면 null */
    @Column(length = 20)
    private String accessory;

    /** 마지막으로 하소연에 쓴 시각. 홈 목록 정렬 기준 */
    @Column(nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime lastUsedAt;

    public static UserCharacter create(User user, CharacterLook look, LocalDateTime now) {
        UserCharacter character = new UserCharacter();
        character.user = user;
        character.apply(look);
        character.lastUsedAt = now;
        return character;
    }

    /** 다시 꾸미기. lastUsedAt은 바꾸지 않는다 */
    public void apply(CharacterLook look) {
        this.name = look.name();
        this.color = look.color();
        this.eyes = look.eyes();
        this.nose = look.nose();
        this.mouth = look.mouth();
        this.eyebrows = look.eyebrows();
        this.accessory = look.accessory();
    }

    public void markUsed(LocalDateTime now) {
        this.lastUsedAt = now;
    }
}
