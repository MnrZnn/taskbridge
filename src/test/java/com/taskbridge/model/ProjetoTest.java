package com.taskbridge.model;

import com.taskbridge.model.enums.ContratacaoStatus;
import com.taskbridge.model.enums.ProjetoStatus;
import com.taskbridge.model.enums.PropostaStatus;
import com.taskbridge.model.Categoria;
import com.taskbridge.model.Contratacao;
import com.taskbridge.model.EscopoProjeto;
import com.taskbridge.model.Projeto;
import com.taskbridge.model.Proposta;
import com.taskbridge.model.Usuario;
import com.taskbridge.exception.RegraNegocioException;
import com.taskbridge.exception.TransicaoEstadoInvalidaException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.taskbridge.model.Fixtures.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjetoTest {

    private final Categoria web = Fixtures.categoria("desenvolvimento-web");
    private final Usuario cliente = Fixtures.cliente();
    private final Usuario prestador = Fixtures.prestador();

    @Nested
    class Publicacao {

        @Test
        void rascunhoIncompletoNaoPublicaEListaAsPendencias() {
            Projeto projeto = new Projeto(cliente, EscopoProjeto.builder("Site", "Preciso de um site").build());

            assertThatThrownBy(() -> projeto.publicar(AGORA))
                    .isInstanceOf(RegraNegocioException.class)
                    .satisfies(erro -> assertThat(((RegraNegocioException) erro).getDetalhes())
                            .contains("Selecione uma categoria.", "Informe a faixa de orçamento.",
                                    "Adicione pelo menos um entregável.", "Adicione pelo menos um critério de aceite."));
            assertThat(projeto.getStatus()).isEqualTo(ProjetoStatus.DRAFT);
        }

        @Test
        void escopoCompletoPublica() {
            Projeto projeto = new Projeto(cliente, Fixtures.escopoCompleto(web).build());

            projeto.publicar(AGORA);

            assertThat(projeto.getStatus()).isEqualTo(ProjetoStatus.PUBLISHED);
            assertThat(projeto.getPublicadoEm()).isEqualTo(AGORA);
        }

        @Test
        void somenteRascunhoPodeSerEditado() {
            Projeto projeto = Fixtures.projetoPublicado(cliente, web);

            assertThatThrownBy(() -> projeto.redefinirEscopo(Fixtures.escopoCompleto(web).build()))
                    .isInstanceOf(RegraNegocioException.class);
        }

        @Test
        void prestadorNaoCriaProjeto() {
            assertThatThrownBy(() -> new Projeto(prestador, Fixtures.escopoCompleto(web).build()))
                    .isInstanceOf(RegraNegocioException.class);
        }
    }

    @Nested
    class Propostas {

        @Test
        void usuarioNaoEnviaPropostaParaOProprioProjeto() {
            Projeto projeto = Fixtures.projetoPublicado(cliente, web);

            assertThatThrownBy(() -> projeto.receberProposta(cliente, new BigDecimal("100"), 10, "mensagem de teste"))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("próprio projeto");
        }

        @Test
        void clienteNaoEnviaProposta() {
            Projeto projeto = Fixtures.projetoPublicado(cliente, web);

            assertThatThrownBy(() -> projeto.receberProposta(Fixtures.cliente(), new BigDecimal("100"), 10, "mensagem de teste"))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Somente prestadores");
        }

        @Test
        void rascunhoNaoRecebePropostas() {
            Projeto rascunho = new Projeto(cliente, Fixtures.escopoCompleto(web).build());

            assertThatThrownBy(() -> rascunho.receberProposta(prestador, new BigDecimal("100"), 10, "mensagem de teste"))
                    .isInstanceOf(RegraNegocioException.class);
        }

        @Test
        void primeiraPropostaColocaProjetoEmNegociacao() {
            Projeto projeto = Fixtures.projetoPublicado(cliente, web);

            projeto.receberProposta(prestador, new BigDecimal("3000"), 20, "mensagem de teste");

            assertThat(projeto.getStatus()).isEqualTo(ProjetoStatus.IN_NEGOTIATION);
        }
    }

    @Nested
    class Aceite {

        @Test
        void aceitarCriaContratacaoComEntregasDosEntregaveis() {
            Projeto projeto = Fixtures.projetoPublicado(cliente, web);
            Proposta proposta = projeto.receberProposta(prestador, new BigDecimal("3000"), 20, "mensagem de teste");

            Contratacao contratacao = projeto.aceitarProposta(proposta, AGORA);

            assertThat(proposta.getStatus()).isEqualTo(PropostaStatus.ACCEPTED);
            assertThat(projeto.getStatus()).isEqualTo(ProjetoStatus.CONTRACTED);
            assertThat(contratacao.getStatus()).isEqualTo(ContratacaoStatus.AWAITING_PAYMENT);
            assertThat(contratacao.getValor()).isEqualByComparingTo("3000");
            assertThat(contratacao.getEntregas()).extracting("titulo").containsExactly("Layout aprovado", "Site publicado");
        }

        @Test
        void propostaAceitaNaoPodeSerAceitaNovamente() {
            Projeto projeto = Fixtures.projetoPublicado(cliente, web);
            Proposta proposta = projeto.receberProposta(prestador, new BigDecimal("3000"), 20, "mensagem de teste");
            projeto.aceitarProposta(proposta, AGORA);

            assertThatThrownBy(() -> projeto.aceitarProposta(proposta, AGORA))
                    .isInstanceOf(TransicaoEstadoInvalidaException.class);
        }

        @Test
        void propostaRecusadaNaoPodeSerAceita() {
            Projeto projeto = Fixtures.projetoPublicado(cliente, web);
            Proposta proposta = projeto.receberProposta(prestador, new BigDecimal("3000"), 20, "mensagem de teste");
            proposta.rejeitar(AGORA);

            assertThatThrownBy(() -> projeto.aceitarProposta(proposta, AGORA))
                    .isInstanceOf(TransicaoEstadoInvalidaException.class);
            assertThat(projeto.getStatus()).isNotEqualTo(ProjetoStatus.CONTRACTED);
        }

        @Test
        void projetoContratadoNaoAceitaSegundaProposta() {
            Projeto projeto = Fixtures.projetoPublicado(cliente, web);
            Proposta primeira = projeto.receberProposta(prestador, new BigDecimal("3000"), 20, "mensagem de teste");
            Proposta segunda = projeto.receberProposta(Fixtures.prestador(), new BigDecimal("2500"), 25, "mensagem de teste");
            projeto.aceitarProposta(primeira, AGORA);

            assertThatThrownBy(() -> projeto.aceitarProposta(segunda, AGORA))
                    .isInstanceOf(TransicaoEstadoInvalidaException.class);
            assertThat(segunda.getStatus()).isEqualTo(PropostaStatus.PENDING);
        }
    }

    @Test
    void projetoPrivadoSoEVisivelParaODono() {
        Projeto rascunho = new Projeto(cliente, Fixtures.escopoCompleto(web).build());

        assertThat(rascunho.visivelPara(cliente.getId())).isTrue();
        assertThat(rascunho.visivelPara(prestador.getId())).isFalse();
        assertThat(rascunho.visivelPara(null)).isFalse();
    }
}
