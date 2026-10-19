package com.taskbridge.model.matching;

/**
 * Resultado de um critério: {@code fator} entre 0 e 1 e uma explicação legível
 * ("Oferece serviços em Desenvolvimento Web"), que é o que a interface mostra ao usuário.
 */
public record AvaliacaoCriterio(String criterio, double fator, String explicacao) {

    private static final double LIMIAR_ATENDIDO = 0.5;

    public AvaliacaoCriterio {
        if (fator < 0 || fator > 1) {
            throw new IllegalArgumentException("fator deve estar entre 0 e 1");
        }
    }

    public static AvaliacaoCriterio atendido(String criterio, String explicacao) {
        return new AvaliacaoCriterio(criterio, 1, explicacao);
    }

    public static AvaliacaoCriterio parcial(String criterio, double fator, String explicacao) {
        return new AvaliacaoCriterio(criterio, fator, explicacao);
    }

    public static AvaliacaoCriterio naoAtendido(String criterio, String explicacao) {
        return new AvaliacaoCriterio(criterio, 0, explicacao);
    }

    public boolean atendido() {
        return fator >= LIMIAR_ATENDIDO;
    }
}
