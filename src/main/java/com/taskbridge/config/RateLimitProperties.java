package com.taskbridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("taskbridge.rate-limit")
public record RateLimitProperties(Limite login, Limite registro, Limite upload, Limite api) {

    public record Limite(int capacidade, Duration periodo) {
    }
}
