package com.taskbridge.model.enums;

public enum PropostaStatus implements Estado<PropostaStatus> {

    PENDING("Pendente"),
    ACCEPTED("Aceita"),
    REJECTED("Recusada"),
    CANCELLED("Cancelada");

    private final String rotulo;

    PropostaStatus(String rotulo) {
        this.rotulo = rotulo;
    }

    /** Só propostas pendentes mudam de estado: aceita, recusada e cancelada são finais. */
    @Override
    public boolean permiteTransicaoPara(PropostaStatus destino) {
        return this == PENDING && destino != PENDING;
    }

    @Override
    public String rotulo() {
        return rotulo;
    }
}
