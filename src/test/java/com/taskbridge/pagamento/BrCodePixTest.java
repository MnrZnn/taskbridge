package com.taskbridge.pagamento;

import com.taskbridge.pagamento.GatewayPagamento.CobrancaPix;
import com.taskbridge.pagamento.GatewayPagamento.SolicitacaoCobranca;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BrCodePixTest {

    @Test
    void crc16UsaAVarianteCcittFalse() {
        // Vetor de verificação padrão do CRC-16/CCITT-FALSE.
        assertThat(BrCodePix.crc16("123456789")).isEqualTo("29B1");
    }

    @Test
    void payloadTemCamposEmvECrcValido() {
        String payload = BrCodePix.builder()
                .chave("pix-teste@taskbridge.invalid")
                .valor(new BigDecimal("150.5"))
                .nomeRecebedor("Loja Açaí & Cia")
                .cidade("São Paulo")
                .txid("ABC123")
                .build();

        assertThat(payload).startsWith("000201010212");
        assertThat(payload).contains("0014br.gov.bcb.pix", "0128pix-teste@taskbridge.invalid");
        assertThat(payload).contains("5406150.50", "5802BR", "5913LOJA ACAI CIA", "6009SAO PAULO", "62100506ABC123");
        String semCrc = payload.substring(0, payload.length() - 4);
        assertThat(semCrc).endsWith("6304");
        assertThat(payload.substring(payload.length() - 4)).isEqualTo(BrCodePix.crc16(semCrc));
    }

    @Test
    void mockNuncaUsaChaveReal() {
        CobrancaPix cobranca = new MockPixGateway().criarCobranca(
                new SolicitacaoCobranca("TXID123", new BigDecimal("10"), "CONTRATACAO 1"));

        assertThat(cobranca.payloadCopiaECola())
                .contains(MockPixGateway.CHAVE_TESTE)
                .contains("TASKBRIDGE TESTE");
        assertThat(MockPixGateway.CHAVE_TESTE).endsWith(".invalid");
    }
}
