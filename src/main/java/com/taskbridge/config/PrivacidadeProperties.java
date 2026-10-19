package com.taskbridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("taskbridge.privacidade")
public record PrivacidadeProperties(String versaoTermos, Duration retencaoAuditoria) {
}
