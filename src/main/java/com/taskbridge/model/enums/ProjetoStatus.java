package com.taskbridge.model.enums;

public enum ProjetoStatus implements Estado<ProjetoStatus> {

    DRAFT("Rascunho"),
    PUBLISHED("Publicado"),
    IN_NEGOTIATION("Em negociação"),
    CONTRACTED("Contratado"),
    IN_PROGRESS("Em andamento"),
    COMPLETED("Concluído"),
    CANCELLED("Cancelado");

    private final String rotulo;

    ProjetoStatus(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public boolean permiteTransicaoPara(ProjetoStatus destino) {
        return switch (this) {
            case DRAFT -> destino == PUBLISHED || destino == CANCELLED;
            case PUBLISHED -> destino == IN_NEGOTIATION || destino == CONTRACTED || destino == CANCELLED;
            case IN_NEGOTIATION -> destino == CONTRACTED || destino == CANCELLED;
            case CONTRACTED -> destino == IN_PROGRESS;
            case IN_PROGRESS -> destino == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    public boolean aceitaPropostas() {
        return this == PUBLISHED || this == IN_NEGOTIATION;
    }

    public boolean visivelPublicamente() {
        return this != DRAFT && this != CANCELLED;
    }

    @Override
    public String rotulo() {
        return rotulo;
    }
}
