package com.taskbridge.service;

import com.taskbridge.config.PagamentoProperties;
import com.taskbridge.model.enums.ModoPagamento;
import com.taskbridge.exception.AcessoNegadoException;
import com.taskbridge.pagamento.GatewayPagamento;
import com.taskbridge.pagamento.GeradorQrCode;
import com.taskbridge.repository.PagamentoRepository;
import com.taskbridge.service.PagamentoService.AcaoSimulada;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A simulação de pagamento tem trava própria no serviço, além do controller não existir em prod.
 * Com Mockito dá para provar que, com debug desligado, nenhum dado sequer é consultado.
 */
class PagamentoServiceTest {

    private final PagamentoRepository repositorio = mock(PagamentoRepository.class);
    private final GatewayPagamento gateway = mock(GatewayPagamento.class);

    @Test
    void simulacaoRecusadaComDebugDesligado() {
        PagamentoService servico = servico(false);
        when(gateway.modo()).thenReturn(ModoPagamento.MOCK);

        assertThatThrownBy(() -> servico.simular(1L, 10L, AcaoSimulada.PAGAR)).isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(repositorio);
    }

    @Test
    void simulacaoRecusadaForaDoModoMock() {
        PagamentoService servico = servico(true);
        when(gateway.modo()).thenReturn(ModoPagamento.REAL);

        assertThatThrownBy(() -> servico.simular(1L, 10L, AcaoSimulada.PAGAR)).isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(repositorio);
    }

    private PagamentoService servico(boolean debug) {
        return new PagamentoService(repositorio, mock(ContratacaoService.class), gateway, mock(GeradorQrCode.class),
                mock(NotificacaoService.class), mock(AuditoriaService.class),
                new PagamentoProperties(ModoPagamento.MOCK, debug, Duration.ofMinutes(30)), Clock.systemUTC());
    }
}
