package com.ganzithon.motdahan;

import static org.assertj.core.api.Assertions.assertThat;

import com.ganzithon.motdahan.global.security.AuthUser;
import com.ganzithon.motdahan.global.security.JwtProperties;
import com.ganzithon.motdahan.global.security.JwtProvider;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private final JwtProvider provider =
            new JwtProvider(new JwtProperties("test-secret-test-secret-test-secret-1234", Duration.ofDays(7)));

    @Test
    void 토큰을_만들고_다시_읽는다() {
        String token = provider.createToken(1L, "민경");

        assertThat(provider.parse(token)).contains(new AuthUser(1L, "민경"));
    }

    @Test
    void 틀린_토큰은_빈_값() {
        assertThat(provider.parse("not-a-token")).isEmpty();
    }

    @Test
    void 만료된_토큰은_빈_값() {
        JwtProvider expired =
                new JwtProvider(new JwtProperties("test-secret-test-secret-test-secret-1234", Duration.ofMillis(-1000)));

        assertThat(expired.parse(expired.createToken(1L, "민경"))).isEmpty();
    }
}
