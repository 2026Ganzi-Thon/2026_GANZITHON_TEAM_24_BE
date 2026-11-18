package com.ganzithon.toro.domain.stt;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganzithon.toro.domain.stt.client.GroqSttClient;
import com.ganzithon.toro.domain.stt.config.GroqProperties;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GroqSttClientTest {
    private MockRestServiceServer server;
    private RestClient http;
    private GroqSttClient client;
    @BeforeEach void setup() {
        var builder = RestClient.builder().baseUrl("https://api.groq.com/openai/v1");
        server = MockRestServiceServer.bindTo(builder).build(); http = builder.build(); client = client("test-key");
    }
    private GroqSttClient client(String key) {
        return new GroqSttClient(http, new ObjectMapper(), new GroqProperties(key,
                "https://api.groq.com/openai/v1", "whisper-large-v3-turbo", Duration.ofSeconds(5), Duration.ofSeconds(45)));
    }

    @Test void 한국어_음성을_일반파일명으로_변환하고_텍스트만_반환한다() {
        server.expect(requestTo("https://api.groq.com/openai/v1/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(request -> {
                    assertThat(request.getHeaders().getContentType().isCompatibleWith(MediaType.MULTIPART_FORM_DATA)).isTrue();
                    String body = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.ISO_8859_1);
                    assertThat(body).contains("filename=\"recording.wav\"", "whisper-large-v3-turbo",
                            "name=\"language\"\r\nContent-Type: text/plain;charset=UTF-8\r\nContent-Length: 2\r\n\r\nko",
                            "verbose_json").doesNotContain("private-person");
                }).andRespond(withSuccess("{\"text\":\"  속상했어요.  \",\"duration\":1.0,\"segments\":[{\"text\":\"provider details\"}]}", MediaType.APPLICATION_JSON));
        var result = client.transcribe(wav());
        assertThat(result.text()).isEqualTo("속상했어요.");
        assertThat(result.durationSeconds()).isEqualTo(1.0);
        assertThat(result.toString()).doesNotContain("속상");
        server.verify();
    }

    @ParameterizedTest @ValueSource(strings = {"webm", "wav", "mp3", "mp4", "m4a", "ogg", "flac"})
    void 지원하는_컨테이너_헤더를_확인한다(String extension) {
        byte[] bytes = new byte[44];
        switch (extension) {
            case "webm" -> { bytes[0] = 0x1a; bytes[1] = 0x45; bytes[2] = (byte) 0xdf; bytes[3] = (byte) 0xa3; }
            case "wav" -> bytes = wavBytes();
            case "mp3" -> put(bytes, 0, "ID3");
            case "mp4", "m4a" -> put(bytes, 4, "ftyp");
            case "ogg" -> put(bytes, 0, "OggS");
            case "flac" -> put(bytes, 0, "fLaC");
        }
        server.expect(anything()).andRespond(withSuccess("{\"text\":\"인식 결과\",\"duration\":60}", MediaType.APPLICATION_JSON));
        assertThat(client.transcribe(new MockMultipartFile("file", "recording." + extension, "application/octet-stream", bytes))
                .durationSeconds()).isEqualTo(60);
        server.verify();
    }

    @Test void 비어있거나_위장한_파일과_크기초과를_업로드_전에_차단한다() {
        error(() -> client.transcribe(new MockMultipartFile("file", new byte[0])), ErrorCode.UNSUPPORTED_AUDIO);
        error(() -> client.transcribe(new MockMultipartFile("file", "note.wav", "audio/wav", "not audio contents".getBytes())), ErrorCode.UNSUPPORTED_AUDIO);
        error(() -> client.transcribe(new MockMultipartFile("file", "note.wav", "text/plain", wavBytes())), ErrorCode.UNSUPPORTED_AUDIO);
        error(() -> client.transcribe(new MockMultipartFile("file", "note.exe", "audio/wav", wavBytes())), ErrorCode.UNSUPPORTED_AUDIO);
        error(() -> client.transcribe(new MockMultipartFile("file", "big.wav", "audio/wav", new byte[10 * 1024 * 1024 + 1])), ErrorCode.AUDIO_TOO_LARGE);
        server.verify();
    }

    @Test void 키가_없으면_외부호출하지_않는다() {
        error(() -> client("").transcribe(wav()), ErrorCode.STT_NOT_CONFIGURED); server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "not-json", "{\"text\":\"말\"}", "{\"text\":123,\"duration\":1}",
            "{\"text\":\"말\",\"duration\":\"1\"}", "{\"text\":\"말\",\"duration\":0}",
            "{\"text\":\"말\",\"duration\":-1}", "{\"text\":\"말\",\"duration\":1e999}",
            "{\"text\":\"말\",\"duration\":1} {}"})
    void 잘못된_공급자응답을_그대로_노출하지_않는다(String response) {
        server.expect(anything()).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        error(() -> client.transcribe(wav()), ErrorCode.STT_FAILED);
    }

    @Test void 초과길이_무음_긴전사문_거절() {
        server.expect(anything()).andRespond(withSuccess("{\"text\":\"말\",\"duration\":60.01}", MediaType.APPLICATION_JSON));
        server.expect(anything()).andRespond(withSuccess("{\"text\":\"  \",\"duration\":1}", MediaType.APPLICATION_JSON));
        server.expect(anything()).andRespond(withSuccess("{\"text\":\"" + "가".repeat(2001) + "\",\"duration\":60}", MediaType.APPLICATION_JSON));
        error(() -> client.transcribe(wav()), ErrorCode.AUDIO_TOO_LARGE);
        error(() -> client.transcribe(wav()), ErrorCode.STT_NO_SPEECH);
        error(() -> client.transcribe(wav()), ErrorCode.INVALID_MESSAGE);
    }

    @ParameterizedTest @CsvSource({"429,STT_BUSY", "413,AUDIO_TOO_LARGE", "400,UNSUPPORTED_AUDIO", "401,STT_FAILED", "500,STT_FAILED"})
    void 공급자_실패_본문은_전파하거나_재시도하지_않는다(int status, String code) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status)).body("private-provider-error"));
        error(() -> client.transcribe(wav()), ErrorCode.valueOf(code)); server.verify();
    }

    @Test void 타임아웃과_대형응답을_안전하게_처리한다() {
        server.expect(anything()).andRespond(withException(new SocketTimeoutException("private raw data")));
        server.expect(anything()).andRespond(withSuccess("x".repeat(262_145), MediaType.APPLICATION_JSON));
        error(() -> client.transcribe(wav()), ErrorCode.STT_FAILED);
        error(() -> client.transcribe(wav()), ErrorCode.STT_FAILED);
    }

    private static MockMultipartFile wav() {
        return new MockMultipartFile("file", "private-person.wav", "audio/wav", wavBytes());
    }
    private static byte[] wavBytes() {
        byte[] bytes = new byte[32044];
        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(bytes.length - 8)
                .put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16)
                .putShort((short) 1).putShort((short) 1).putInt(16000).putInt(32000)
                .putShort((short) 2).putShort((short) 16).put("data".getBytes(StandardCharsets.US_ASCII)).putInt(32000);
        return bytes;
    }
    private static void put(byte[] bytes, int offset, String header) {
        System.arraycopy(header.getBytes(StandardCharsets.US_ASCII), 0, bytes, offset, header.length());
    }
    private static void error(Runnable run, ErrorCode code) {
        assertThatThrownBy(run::run).isInstanceOfSatisfying(BusinessException.class, e -> {
            assertThat(e.getErrorCode()).isEqualTo(code);
            assertThat(e.getCause()).isNull();
            assertThat(e.getMessage()).doesNotContain("private");
        });
    }
}
