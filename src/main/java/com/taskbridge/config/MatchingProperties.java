package com.taskbridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("taskbridge.matching")
public record MatchingProperties(
        int pesoCategoria,
        int pesoHabilidades,
        int pesoOrcamento,
        int pesoPrazo,
        int pesoDisponibilidade,
        int limiteCandidatos) {
}
