package com.ganzithon.toro.domain.stt.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganzithon.toro.domain.stt.config.GroqProperties;
import com.ganzithon.toro.domain.stt.dto.Transcription;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Component
public class GroqSttClient {
    private static final int MAX_AUDIO_BYTES = 10 * 1024 * 1024;
    private static final int MAX_RESPONSE_BYTES = 262_144;
    private static final Map<String, Set<String>> TYPES = Map.of(
            "webm", Set.of("audio/webm", "video/webm"),
            "wav", Set.of("audio/wav", "audio/x-wav", "audio/wave"),
            "mp3", Set.of("audio/mpeg", "audio/mp3"),
            "mp4", Set.of("audio/mp4", "video/mp4"),
            "m4a", Set.of("audio/mp4", "audio/x-m4a", "audio/m4a"),
            "ogg", Set.of("audio/ogg", "application/ogg"),
            "flac", Set.of("audio/flac", "audio/x-flac"));
    private final RestClient client;
    private final ObjectMapper mapper;
    private final GroqProperties properties;

    public GroqSttClient(@Qualifier("groqRestClient") RestClient client, ObjectMapper mapper,
                         GroqProperties properties) {
        this.client = client; this.mapper = mapper; this.properties = properties;
    }

    public Transcription transcribe(MultipartFile file) {
        byte[] audio = null;
        try {
            if (file.isEmpty()) fail(ErrorCode.UNSUPPORTED_AUDIO);
            if (file.getSize() > MAX_AUDIO_BYTES) fail(ErrorCode.AUDIO_TOO_LARGE);
            String extension = extension(file);
            try (var stream = file.getInputStream()) { audio = stream.readNBytes(MAX_AUDIO_BYTES + 1); }
            if (audio.length > MAX_AUDIO_BYTES) fail(ErrorCode.AUDIO_TOO_LARGE);
            if (!hasHeader(audio, extension)) fail(ErrorCode.UNSUPPORTED_AUDIO);
            if (properties.apiKey() == null || properties.apiKey().isBlank()) fail(ErrorCode.STT_NOT_CONFIGURED);

            // 원래 파일명(개인정보 가능)을 보내지 않는다. 디스크 파일/업로드 URL을 만들지 않는다.
            var resource = new ByteArrayResource(audio) {
                @Override public String getFilename() { return "recording." + extension; }
            };
            var form = new LinkedMultiValueMap<String, Object>();
            form.add("file", resource);
            form.add("model", properties.model());
            form.add("language", "ko");
            form.add("response_format", "verbose_json");
            form.add("temperature", "0");
            return client.post().uri("/audio/transcriptions")
                    .headers(h -> h.setBearerAuth(properties.apiKey().strip()))
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(form)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 429) fail(ErrorCode.STT_BUSY);
                        if (status == 413) fail(ErrorCode.AUDIO_TOO_LARGE);
                        if (status == 400 || status == 422) fail(ErrorCode.UNSUPPORTED_AUDIO);
                        if (!response.getStatusCode().is2xxSuccessful()) fail(ErrorCode.STT_FAILED);
                        byte[] bytes = response.getBody().readNBytes(MAX_RESPONSE_BYTES + 1);
                        try {
                            if (bytes.length > MAX_RESPONSE_BYTES) fail(ErrorCode.STT_FAILED);
                            JsonNode root = mapper.readerFor(JsonNode.class)
                                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(bytes);
                            if (root == null || !root.isObject() || !root.path("text").isTextual()
                                    || !root.path("duration").isNumber()) fail(ErrorCode.STT_FAILED);
                            double duration = root.path("duration").doubleValue();
                            if (!Double.isFinite(duration) || duration <= 0) fail(ErrorCode.STT_FAILED);
                            // 클라이언트의 duration은 신뢰하지 않는다. 공급자가 판독한 길이로 최종 검증한다.
                            if (duration > 60) fail(ErrorCode.AUDIO_TOO_LARGE);
                            String text = root.path("text").textValue().strip();
                            if (text.isEmpty()) fail(ErrorCode.STT_NO_SPEECH);
                            if (text.length() > 2000) fail(ErrorCode.INVALID_MESSAGE);
                            return new Transcription(text, duration);
                        } finally { Arrays.fill(bytes, (byte) 0); }
                    });
        } catch (BusinessException e) {
            throw e;
        } catch (Exception ignored) {
            // 오류 원문, 파일명, API 키를 로그 또는 응답에 포함하지 않는다.
            throw new BusinessException(ErrorCode.STT_FAILED);
        } finally {
            if (audio != null) Arrays.fill(audio, (byte) 0);
        }
    }

    private static String extension(MultipartFile file) {
        String name = file.getOriginalFilename();
        String ext = name == null ? "" : name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!TYPES.containsKey(ext)) fail(ErrorCode.UNSUPPORTED_AUDIO);
        String mime = file.getContentType();
        // 브라우저가 MIME을 생략하거나 octet-stream으로 보내도 헤더 검사를 반드시 거친다.
        if (mime != null) {
            mime = mime.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
            if (!mime.equals("application/octet-stream") && !TYPES.get(ext).contains(mime))
                fail(ErrorCode.UNSUPPORTED_AUDIO);
        }
        return ext;
    }

    private static boolean hasHeader(byte[] b, String ext) {
        if (b.length < 12) return false;
        return switch (ext) {
            case "webm" -> (b[0] & 255) == 0x1a && (b[1] & 255) == 0x45
                    && (b[2] & 255) == 0xdf && (b[3] & 255) == 0xa3;
            case "wav" -> starts(b, 0, "RIFF") && starts(b, 8, "WAVE");
            case "mp4", "m4a" -> starts(b, 4, "ftyp");
            case "ogg" -> starts(b, 0, "OggS");
            case "flac" -> starts(b, 0, "fLaC");
            case "mp3" -> starts(b, 0, "ID3") || ((b[0] & 255) == 0xff && (b[1] & 0xe0) == 0xe0);
            default -> false;
        };
    }
    private static boolean starts(byte[] bytes, int offset, String text) {
        byte[] expected = text.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < expected.length; i++) if (bytes[offset + i] != expected[i]) return false;
        return true;
    }
    private static void fail(ErrorCode code) { throw new BusinessException(code); }
}
