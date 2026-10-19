package com.taskbridge.pagamento;

import com.taskbridge.model.enums.ModoPagamento;
import com.taskbridge.exception.PagamentoIndisponivelException;
import com.taskbridge.pagamento.GatewayPagamento.SolicitacaoCobranca;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayPagamentoTest {

    private final SolicitacaoCobranca solicitacao = new SolicitacaoCobranca("TXID0001", new BigDecimal("99.90"), "teste");

    @Test
    void factoryCriaMockDecorado() {
        GatewayPagamento gateway = new GatewayPagamentoFactory().criar(ModoPagamento.MOCK);

        assertThat(gateway).isInstanceOf(GatewayPagamentoMonitorado.class);
        assertThat(gateway.modo()).isEqualTo(ModoPagamento.MOCK);
        assertThat(gateway.criarCobranca(solicitacao).txid()).isEqualTo("TXID0001");
    }

    @Test
    void modoRealSemAdapterFalhaEmVezDeFingirIntegracao() {
        assertThatThrownBy(() -> new GatewayPagamentoFactory().criar(ModoPagamento.REAL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PAYMENT_MODE=REAL");
    }

    @Test
    void decoratorRepassaAChamadaAoGatewayDecorado() {
        GatewayPagamento delegado = mock(GatewayPagamento.class);
        when(delegado.criarCobranca(any())).thenReturn(new GatewayPagamento.CobrancaPix("TXID0001", "payload"));

        new GatewayPagamentoMonitorado(delegado).criarCobranca(solicitacao);

        verify(delegado).criarCobranca(solicitacao);
    }

    @Test
    void decoratorTraduzFalhaTecnicaDoProvedor() {
        GatewayPagamento delegado = mock(GatewayPagamento.class);
        when(delegado.criarCobranca(any())).thenThrow(new IllegalStateException("timeout no PSP"));

        assertThatThrownBy(() -> new GatewayPagamentoMonitorado(delegado).criarCobranca(solicitacao))
                .isInstanceOf(PagamentoIndisponivelException.class);
    }

    @Test
    void txidApareceMascaradoNoLog() {
        assertThat(GatewayPagamentoMonitorado.mascarar("ABCDEFGHIJ1234567890")).isEqualTo("ABCD…90");
    }
}
