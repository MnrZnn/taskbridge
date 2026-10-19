package com.taskbridge.config;

import com.taskbridge.pagamento.GatewayPagamento;
import com.taskbridge.pagamento.GatewayPagamentoFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PagamentoConfig {

    @Bean
    GatewayPagamento gatewayPagamento(PagamentoProperties pagamento) {
        return new GatewayPagamentoFactory().criar(pagamento.modo());
    }
}
