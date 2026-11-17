package com.ganzithon.toro.domain.vent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(UpstageProperties.class)
public class UpstageConfig {

    @Bean
    public RestClient upstageRestClient(UpstageProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        // 별도의 클라이언트: 본문 로깅 인터셉터와 자동 재시도를 사용하지 않는다.
        return RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }
}
