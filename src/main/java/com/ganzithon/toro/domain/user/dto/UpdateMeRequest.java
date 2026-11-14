package com.ganzithon.toro.domain.user.dto;

import com.ganzithon.toro.domain.user.UserRules;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 203 닉네임·홈 배경 수정. 보낸 필드만 바꾼다(null이면 그대로).
 * 배경은 사용자 전체에 1개. 허용 값은 CharacterOptions.BACKGROUND (없는 값이면 CHAR_002).
 */
public record UpdateMeRequest(
        @Pattern(regexp = UserRules.NAME_REGEX, message = UserRules.NAME_MESSAGE) String nickname,
        @Schema(allowableValues = {"READING", "TERRACE", "KITCHEN", "STUDIO", "GREEN", "DEFAULT"})
        @Size(min = 1, max = UserRules.BACKGROUND_MAX_LENGTH) String background
) {
    public UpdateMeRequest {
        nickname = UserRules.strip(nickname);
        background = UserRules.strip(background);
    }
}
