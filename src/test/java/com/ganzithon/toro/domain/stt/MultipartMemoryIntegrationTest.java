package com.ganzithon.toro.domain.stt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.ganzithon.toro.domain.stt.client.GroqSttClient;
import com.ganzithon.toro.domain.stt.controller.SttController;
import com.ganzithon.toro.domain.stt.dto.Transcription;
import com.ganzithon.toro.domain.user.repository.UserRepository;
import com.ganzithon.toro.domain.vent.service.VentSessionStore;
import com.ganzithon.toro.global.config.SecurityConfig;
import com.ganzithon.toro.global.exception.GlobalExceptionHandler;
import com.ganzithon.toro.global.security.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/** MockMvc는 실제 multipart 파서를 지나지 않으므로 실제 Tomcat으로 디스크 미사용을 검증한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = MultipartMemoryIntegrationTest.TestApplication.class,
        properties = {"jwt.secret=test-secret-test-secret-test-secret-1234", "jwt.expiration=7d",
                "cors.allowed-origins=http://localhost:5173"})
@ActiveProfiles("test")
class MultipartMemoryIntegrationTest {
    @TempDir static Path uploads;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.servlet.multipart.location", () -> uploads.toString());
    }
    @LocalServerPort int port;
    @Autowired JwtProvider jwt;
    @Autowired VentSessionStore sessions;
    @MockitoBean UserRepository users;
    @MockitoBean GroqSttClient groq;

    @BeforeEach void setup() { sessions.clear(); given(users.existsById(1L)).willReturn(true); }

    @Test void 실제_업로드_처리중에도_허용파일이_디스크에_생성되지_않는다() {
        given(groq.transcribe(any())).willAnswer(invocation -> {
            try (var paths = Files.list(uploads)) { assertThat(paths.toList()).isEmpty(); }
            return new Transcription("모의 결과", 1);
        });
        assertThat(upload(1024 * 1024)).isEqualTo(200);
        verify(groq).transcribe(any());
    }

    @Test void 파일제한_초과는_외부호출_전에_거절된다() throws Exception {
        assertThat(upload(10 * 1024 * 1024 + 1)).isEqualTo(400);
        verifyNoInteractions(groq);
        try (var paths = Files.list(uploads)) { assertThat(paths.toList()).isEmpty(); }
    }

    private int upload(int size) {
        var form = new LinkedMultiValueMap<String, Object>();
        form.add("sessionId", sessions.create(1).sessionId().toString());
        form.add("file", new ByteArrayResource(new byte[size]) {
            @Override public String getFilename() { return "recording.wav"; }
        });
        return RestClient.create().post().uri("http://localhost:" + port + "/api/v1/stt")
                .headers(headers -> headers.setBearerAuth(jwt.createToken(1L)))
                .contentType(MediaType.MULTIPART_FORM_DATA).body(form)
                .exchange((request, response) -> response.getStatusCode().value());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
    @Import({SttController.class, VentSessionStore.class, SecurityConfig.class, JwtProvider.class,
            JwtAuthenticationFilter.class, SecurityErrorHandler.class, GlobalExceptionHandler.class})
    static class TestApplication { }
}
