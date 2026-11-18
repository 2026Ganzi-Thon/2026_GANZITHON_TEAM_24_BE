package com.ganzithon.toro.domain.vent.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "upstage")
public record UpstageProperties(String apiKey, String baseUrl, String model,
                                Duration connectTimeout, Duration readTimeout) {

    @Override
    public String toString() {
        return "UpstageProperties[redacted]";
    }
}
