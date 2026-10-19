package com.taskbridge.model.matching;

import com.taskbridge.model.FaixaValor;
import com.taskbridge.model.Projeto;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class CompatibilidadeOrcamento implements CriterioCompatibilidade {

    /** Até 20% acima do teto do cliente ainda vale meia pontuação: há margem para negociar. */
    private static final BigDecimal TOLERANCIA = new BigDecimal("1.20");

    @Override
    public String nome() {
        return "Orçamento";
    }

    @Override
    public AvaliacaoCriterio avaliar(Projeto projeto, CandidatoProfissional candidato) {
        FaixaValor orcamento = projeto.getOrcamento();
        if (orcamento == null) {
            return AvaliacaoCriterio.naoAtendido(nome(), "Projeto sem orçamento definido");
        }
        FaixaValor precoCandidato = candidato.perfil().getFaixaPreco();
        String faixa = moeda(precoCandidato.getMinimo()) + " – " + moeda(precoCandidato.getMaximo());
        if (orcamento.sobrepoe(precoCandidato)) {
            return AvaliacaoCriterio.atendido(nome(), "Faixa de preço (" + faixa + ") compatível com o orçamento");
        }
        if (precoCandidato.getMaximo().compareTo(orcamento.getMinimo()) < 0) {
            return AvaliacaoCriterio.atendido(nome(), "Faixa de preço (" + faixa + ") abaixo do orçamento");
        }
        BigDecimal tetoTolerado = orcamento.getMaximo().multiply(TOLERANCIA);
        if (precoCandidato.getMinimo().compareTo(tetoTolerado) <= 0) {
            return AvaliacaoCriterio.parcial(nome(), 0.5, "Faixa de preço (" + faixa + ") um pouco acima do orçamento");
        }
        return AvaliacaoCriterio.naoAtendido(nome(), "Faixa de preço (" + faixa + ") acima do orçamento");
    }

    private static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(valor);
    }
}
