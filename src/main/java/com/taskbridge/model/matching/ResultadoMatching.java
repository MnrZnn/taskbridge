package com.taskbridge.model.matching;

import java.util.List;

public record ResultadoMatching(int pontuacao, List<CriterioAvaliado> criterios) {

    public ResultadoMatching {
        criterios = List.copyOf(criterios);
    }

    public record CriterioAvaliado(AvaliacaoCriterio avaliacao, int peso) {

        public double pontos() {
            return avaliacao.fator() * peso;
        }
    }
}
