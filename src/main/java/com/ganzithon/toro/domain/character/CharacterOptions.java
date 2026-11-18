package com.ganzithon.toro.domain.character;

import com.ganzithon.toro.domain.character.entity.CharacterLook;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 꾸미기 선택지 허용 목록 (2026-11-15 프론트와 확정).
 * 눈·코·입·눈썹은 STYLE_1~STYLE_6, 악세서리는 STYLE_1~STYLE_6 + NIGHTCAP, 색상 7개, 배경 6개. 목록에 없는 값이면 CHAR_002.
 * 기본값(처음 화면에서 무엇이 선택돼 있는지)은 프론트가 정한다. 서버는 저장할 때 들어온 값만 검사한다.
 * 코·악세서리는 선택 안 함이 가능하다: 프론트는 ""를 보내고, DB에는 null, 응답은 "".
 * 값을 바꾸면 DTO의 @Schema(allowableValues)와 노션 명세도 같이 고친다.
 */
public final class CharacterOptions {

    private static final Set<String> STYLES = IntStream.rangeClosed(1, 6)
            .mapToObj(i -> "STYLE_" + i)
            .collect(Collectors.toUnmodifiableSet());

    public static final Set<String> EYES = STYLES;
    /** 안 고르면 null(응답은 "") */
    public static final Set<String> NOSE = STYLES;
    public static final Set<String> MOUTH = STYLES;
    public static final Set<String> EYEBROWS = STYLES;
    /** STYLE_1~6 + NIGHTCAP (2026-11-18 추가). 안 고르면 null(응답은 "") */
    public static final Set<String> ACCESSORY = Stream.concat(STYLES.stream(), Stream.of("NIGHTCAP"))
            .collect(Collectors.toUnmodifiableSet());
    public static final Set<String> COLOR = Set.of("PEACH", "PINK", "YELLOW", "GRAY", "BLUE", "MINT", "LAVENDER");
    /** 배경은 캐릭터가 아니라 사용자 전체 설정(users.background) */
    public static final Set<String> BACKGROUND = Set.of("READING", "TERRACE", "KITCHEN", "STUDIO", "GREEN", "DEFAULT");

    private CharacterOptions() {
    }

    public static void validate(CharacterLook look) {
        check(EYES, look.eyes());
        check(MOUTH, look.mouth());
        check(EYEBROWS, look.eyebrows());
        check(COLOR, look.color());
        // 코·악세서리는 선택 안 함(null)이 가능하다
        if (look.nose() != null) {
            check(NOSE, look.nose());
        }
        if (look.accessory() != null) {
            check(ACCESSORY, look.accessory());
        }
    }

    public static void validateBackground(String background) {
        check(BACKGROUND, background);
    }

    private static void check(Set<String> allowed, String value) {
        if (!allowed.contains(value)) {
            throw new BusinessException(ErrorCode.INVALID_CHARACTER_OPTION);
        }
    }
}
