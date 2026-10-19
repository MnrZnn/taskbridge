package com.taskbridge.model.matching;

import com.taskbridge.model.Projeto;

import java.util.List;

/**
 * Combina as estratégias: pontuação = soma(fator × peso) / soma(pesos), em 0–100.
 * Determinística de propósito — mesmo projeto e mesmo perfil sempre dão o mesmo resultado,
 * o que facilita testar e explicar.
 */
public class CalculadoraCompatibilidade {

    private final List<CriterioPonderado> criterios;
    private final int somaDosPesos;

    public CalculadoraCompatibilidade(List<CriterioPonderado> criterios) {
        this.criterios = List.copyOf(criterios);
        this.somaDosPesos = criterios.stream().mapToInt(CriterioPonderado::peso).sum();
        if (somaDosPesos <= 0) {
            throw new IllegalArgumentException("A soma dos pesos do matching deve ser positiva.");
        }
    }

    public ResultadoMatching calcular(Projeto projeto, CandidatoProfissional candidato) {
        List<ResultadoMatching.CriterioAvaliado> avaliados = criterios.stream()
                .map(ponderado -> ponderado.avaliar(projeto, candidato))
                .toList();
        double pontos = avaliados.stream().mapToDouble(ResultadoMatching.CriterioAvaliado::pontos).sum();
        int pontuacao = (int) Math.round(pontos * 100 / somaDosPesos);
        return new ResultadoMatching(pontuacao, avaliados);
    }

    public record CriterioPonderado(CriterioCompatibilidade criterio, int peso) {

        public CriterioPonderado {
            if (peso < 0) {
                throw new IllegalArgumentException("Peso não pode ser negativo: " + criterio.nome());
            }
        }

        ResultadoMatching.CriterioAvaliado avaliar(Projeto projeto, CandidatoProfissional candidato) {
            AvaliacaoCriterio avaliacao = criterio.avaliar(projeto, candidato);
            return new ResultadoMatching.CriterioAvaliado(avaliacao, peso);
        }
    }
}
