package com.taskbridge.model;

import com.taskbridge.model.enums.ContratacaoStatus;
import com.taskbridge.model.enums.EntregaStatus;
import com.taskbridge.model.enums.ModoPagamento;
import com.taskbridge.model.enums.PagamentoStatus;
import com.taskbridge.model.enums.ProjetoStatus;
import com.taskbridge.model.Contratacao;
import com.taskbridge.model.Entrega;
import com.taskbridge.model.Pagamento;
import com.taskbridge.model.Projeto;
import com.taskbridge.model.Proposta;
import com.taskbridge.exception.RegraNegocioException;
import com.taskbridge.exception.TransicaoEstadoInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static com.taskbridge.model.Fixtures.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContratacaoTest {

    private Projeto projeto;
    private Contratacao contratacao;
    private Long primeira;
    private Long segunda;

    @BeforeEach
    void preparar() {
        projeto = Fixtures.projetoPublicado(Fixtures.cliente(), Fixtures.categoria("desenvolvimento-web"));
        Proposta proposta = projeto.receberProposta(Fixtures.prestador(), new BigDecimal("3000"), 20, "mensagem de teste");
        contratacao = projeto.aceitarProposta(proposta, AGORA);
        List<Entrega> entregas = contratacao.getEntregas();
        primeira = Fixtures.comId(entregas.get(0), 101L).getId();
        segunda = Fixtures.comId(entregas.get(1), 102L).getId();
    }

    @Test
    void entregasSoSaoEnviadasDepoisDoPagamento() {
        assertThatThrownBy(() -> contratacao.enviarEntrega(primeira, "link", AGORA))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void confirmarPagamentoIniciaContratacaoEProjeto() {
        pagar();

        assertThat(contratacao.getStatus()).isEqualTo(ContratacaoStatus.IN_PROGRESS);
        assertThat(projeto.getStatus()).isEqualTo(ProjetoStatus.IN_PROGRESS);
    }

    @Test
    void entregaSoPodeSerAprovadaDepoisDeEnviada() {
        pagar();

        assertThatThrownBy(() -> contratacao.aprovarEntrega(primeira, AGORA))
                .isInstanceOf(TransicaoEstadoInvalidaException.class);
    }

    @Test
    void entregaAprovadaNaoVoltaAtras() {
        pagar();
        contratacao.enviarEntrega(primeira, "link do layout", AGORA);
        contratacao.aprovarEntrega(primeira, AGORA);

        assertThatThrownBy(() -> contratacao.enviarEntrega(primeira, "de novo", AGORA))
                .isInstanceOf(TransicaoEstadoInvalidaException.class);
        assertThatThrownBy(() -> contratacao.solicitarRevisao(primeira, "refazer", AGORA))
                .isInstanceOf(TransicaoEstadoInvalidaException.class);
    }

    @Test
    void revisaoSolicitadaPermiteReenvio() {
        pagar();
        contratacao.enviarEntrega(primeira, "v1", AGORA);
        contratacao.solicitarRevisao(primeira, "Ajustar cores", AGORA);

        Entrega reenviada = contratacao.enviarEntrega(primeira, "v2", AGORA);

        assertThat(reenviada.getStatus()).isEqualTo(EntregaStatus.SUBMITTED);
        assertThat(reenviada.getConteudoEnvio()).isEqualTo("v2");
    }

    @Test
    void aprovarTodasAsEntregasConcluiContratacaoEProjeto() {
        pagar();
        contratacao.enviarEntrega(primeira, "layout", AGORA);
        contratacao.enviarEntrega(segunda, "site", AGORA);
        contratacao.aprovarEntrega(primeira, AGORA);

        assertThat(contratacao.getStatus()).isEqualTo(ContratacaoStatus.IN_PROGRESS);

        contratacao.aprovarEntrega(segunda, AGORA);

        assertThat(contratacao.getStatus()).isEqualTo(ContratacaoStatus.COMPLETED);
        assertThat(contratacao.getConcluidaEm()).isEqualTo(AGORA);
        assertThat(projeto.getStatus()).isEqualTo(ProjetoStatus.COMPLETED);
    }

    @Test
    void somenteParticipantesTemAcesso() {
        assertThat(contratacao.temParticipante(contratacao.getCliente().getId())).isTrue();
        assertThat(contratacao.temParticipante(contratacao.getPrestador().getId())).isTrue();
        assertThat(contratacao.temParticipante(Fixtures.cliente().getId())).isFalse();
    }

    @Test
    void pagamentoVencidoNaoPodeSerConfirmado() {
        Pagamento pagamento = new Pagamento(contratacao, "TXID", ModoPagamento.MOCK, AGORA, AGORA.plus(Duration.ofMinutes(30)));
        pagamento.registrarCobranca("payload");

        assertThatThrownBy(() -> pagamento.confirmar(AGORA.plus(Duration.ofMinutes(31))))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(pagamento.getStatus()).isEqualTo(PagamentoStatus.PENDING);
        assertThat(contratacao.getStatus()).isEqualTo(ContratacaoStatus.AWAITING_PAYMENT);
    }

    private void pagar() {
        Pagamento pagamento = new Pagamento(contratacao, "TXID", ModoPagamento.MOCK, AGORA, AGORA.plus(Duration.ofMinutes(30)));
        pagamento.registrarCobranca("payload");
        pagamento.confirmar(AGORA.plusSeconds(60));
    }
}
