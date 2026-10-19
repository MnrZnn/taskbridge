package com.taskbridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("taskbridge.seguranca")
public record SegurancaProperties(
        int bcryptForca,
        int maxTentativasLogin,
        Duration duracaoBloqueioLogin,
        DataSize maxCorpoJson,
        List<String> corsOrigens) {

    public SegurancaProperties {
        corsOrigens = corsOrigens == null ? List.of() : corsOrigens.stream().filter(origem -> !origem.isBlank()).toList();
    }
}
