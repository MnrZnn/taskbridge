package com.taskbridge.config;

import com.taskbridge.model.enums.ModoPagamento;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("taskbridge.pagamento")
public record PagamentoProperties(ModoPagamento modo, boolean debugHabilitado, Duration validadeCobranca) {
}
