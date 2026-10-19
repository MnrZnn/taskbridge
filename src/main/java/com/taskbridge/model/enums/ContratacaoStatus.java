package com.taskbridge.model.enums;

public enum ContratacaoStatus implements Estado<ContratacaoStatus> {

    AWAITING_PAYMENT("Aguardando pagamento"),
    IN_PROGRESS("Em andamento"),
    COMPLETED("Concluída"),
    CANCELLED("Cancelada");

    private final String rotulo;

    ContratacaoStatus(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public boolean permiteTransicaoPara(ContratacaoStatus destino) {
        return switch (this) {
            case AWAITING_PAYMENT -> destino == IN_PROGRESS || destino == CANCELLED;
            case IN_PROGRESS -> destino == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    public boolean ativa() {
        return this == AWAITING_PAYMENT || this == IN_PROGRESS;
    }

    @Override
    public String rotulo() {
        return rotulo;
    }
}
