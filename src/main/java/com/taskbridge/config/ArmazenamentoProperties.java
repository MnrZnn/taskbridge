package com.taskbridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("taskbridge.armazenamento")
public record ArmazenamentoProperties(Path diretorio) {
}
