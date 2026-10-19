package com.taskbridge.model.matching;

import com.taskbridge.model.Fixtures;
import com.taskbridge.model.enums.Disponibilidade;
import com.taskbridge.model.matching.CalculadoraCompatibilidade.CriterioPonderado;
import com.taskbridge.model.Categoria;
import com.taskbridge.model.FaixaValor;
import com.taskbridge.model.PerfilProfissional;
import com.taskbridge.model.Projeto;
import com.taskbridge.model.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraCompatibilidadeTest {

    private final Categoria web = Fixtures.categoria("desenvolvimento-web");
    private final Categoria design = Fixtures.categoria("design-grafico");
    private final Projeto projeto = Fixtures.projetoPublicado(Fixtures.cliente(), web);

    private final CalculadoraCompatibilidade calculadora = new CalculadoraCompatibilidade(List.of(
            new CriterioPonderado(new CompatibilidadeCategoria(), 30),
            new CriterioPonderado(new CompatibilidadeHabilidades(), 25),
            new CriterioPonderado(new CompatibilidadeOrcamento(), 20),
            new CriterioPonderado(new CompatibilidadePrazo(), 15),
            new CriterioPonderado(new CompatibilidadeDisponibilidade(), 10)));

    @Test
    void candidatoIdealPontua100EExplicaCadaCriterio() {
        Usuario prestador = Fixtures.prestador();
        PerfilProfissional perfil = Fixtures.perfil(prestador, Disponibilidade.IMEDIATA, "1500", "3500", "html", "css", "java");
        CandidatoProfissional candidato = new CandidatoProfissional(perfil, List.of(Fixtures.servico(prestador, web, 20)));

        ResultadoMatching resultado = calculadora.calcular(projeto, candidato);

        assertThat(resultado.pontuacao()).isEqualTo(100);
        // NumberFormat pt-BR separa "R$" do valor com espaço não separável; normalizado para comparar.
        assertThat(resultado.criterios()).extracting(avaliado -> avaliado.avaliacao().explicacao().replace(' ', ' '))
                .containsExactly(
                        "Oferece serviços em Categoria desenvolvimento-web",
                        "Domina 2 de 2 habilidades pedidas (html, css)",
                        "Faixa de preço (R$ 1.500,00 – R$ 3.500,00) compatível com o orçamento",
                        "Entrega estimada em 20 dias (prazo do projeto: 30)",
                        "Disponibilidade: imediata");
    }

    @Test
    void outraCategoriaEHabilidadeParcialReduzemPontuacao() {
        Usuario prestador = Fixtures.prestador();
        PerfilProfissional perfil = Fixtures.perfil(prestador, Disponibilidade.IMEDIATA, "1500", "3500", "html");
        CandidatoProfissional candidato = new CandidatoProfissional(perfil, List.of(Fixtures.servico(prestador, design, 20)));

        ResultadoMatching resultado = calculadora.calcular(projeto, candidato);

        // categoria 0/30, habilidades 12,5/25, orçamento 20, prazo 15, disponibilidade 10 → 57,5
        assertThat(resultado.pontuacao()).isEqualTo(58);
        assertThat(resultado.criterios().getFirst().avaliacao().atendido()).isFalse();
    }

    @Test
    void orcamentoPoucoAcimaValeMeiaPontuacao() {
        CompatibilidadeOrcamento criterio = new CompatibilidadeOrcamento();
        PerfilProfissional perfil = Fixtures.perfil(Fixtures.prestador(), Disponibilidade.IMEDIATA, "4500", "9000");

        AvaliacaoCriterio avaliacao = criterio.avaliar(projeto, new CandidatoProfissional(perfil, List.of()));

        assertThat(avaliacao.fator()).isEqualTo(0.5);
        assertThat(avaliacao.explicacao()).contains("um pouco acima");
    }

    @Test
    void orcamentoMuitoAcimaNaoPontua() {
        PerfilProfissional perfil = Fixtures.perfil(Fixtures.prestador(), Disponibilidade.IMEDIATA, "10000", "20000");

        AvaliacaoCriterio avaliacao = new CompatibilidadeOrcamento().avaliar(projeto, new CandidatoProfissional(perfil, List.of()));

        assertThat(avaliacao.fator()).isZero();
    }

    @Test
    void prazoConsideraDisponibilidadeMaisPrazoDoServico() {
        Usuario prestador = Fixtures.prestador();
        PerfilProfissional perfil = Fixtures.perfil(prestador, Disponibilidade.EM_ATE_15_DIAS, "1000", "2000");
        CandidatoProfissional candidato = new CandidatoProfissional(perfil, List.of(Fixtures.servico(prestador, web, 20)));

        AvaliacaoCriterio avaliacao = new CompatibilidadePrazo().avaliar(projeto, candidato);

        assertThat(avaliacao.explicacao()).isEqualTo("Entrega estimada em 35 dias (prazo do projeto: 30)");
        assertThat(avaliacao.fator()).isEqualTo(0.5);
    }

    @Test
    void pesosSaoConfiguraveis() {
        CalculadoraCompatibilidade somenteCategoria = new CalculadoraCompatibilidade(List.of(
                new CriterioPonderado(new CompatibilidadeCategoria(), 1),
                new CriterioPonderado(new CompatibilidadeHabilidades(), 0)));
        Usuario prestador = Fixtures.prestador();
        PerfilProfissional perfil = Fixtures.perfil(prestador, Disponibilidade.IMEDIATA, "1", "2");

        ResultadoMatching resultado = somenteCategoria.calcular(projeto,
                new CandidatoProfissional(perfil, List.of(Fixtures.servico(prestador, web, 5))));

        assertThat(resultado.pontuacao()).isEqualTo(100);
    }

    @Test
    void faixasDeValorSobrepostas() {
        FaixaValor orcamento = new FaixaValor(new BigDecimal("2000"), new BigDecimal("4000"));

        assertThat(orcamento.sobrepoe(new FaixaValor(new BigDecimal("4000"), new BigDecimal("5000")))).isTrue();
        assertThat(orcamento.sobrepoe(new FaixaValor(new BigDecimal("4000.01"), new BigDecimal("5000")))).isFalse();
    }
}
