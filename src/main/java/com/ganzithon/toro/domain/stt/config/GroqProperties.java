package com.ganzithon.toro.domain.stt.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "groq")
public record GroqProperties(String apiKey, String baseUrl, String model,
                             Duration connectTimeout, Duration readTimeout) {
    @Override public String toString() { return "GroqProperties[redacted]"; }
}
