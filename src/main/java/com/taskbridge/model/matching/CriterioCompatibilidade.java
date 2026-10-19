package com.taskbridge.model.matching;

import com.taskbridge.model.Projeto;

/**
 * Strategy do matching. Cada critério avalia um aspecto isolado e explica o resultado;
 * a {@link CalculadoraCompatibilidade} combina os critérios com os pesos configurados.
 * Um critério novo (ex.: localização) é uma classe nova, sem mexer nas existentes.
 */
public interface CriterioCompatibilidade {

    String nome();

    AvaliacaoCriterio avaliar(Projeto projeto, CandidatoProfissional candidato);
}
