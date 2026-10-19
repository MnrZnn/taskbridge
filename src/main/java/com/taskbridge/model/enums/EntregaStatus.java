package com.taskbridge.model.enums;

public enum EntregaStatus implements Estado<EntregaStatus> {

    PENDING("Pendente"),
    SUBMITTED("Enviada"),
    UNDER_REVIEW("Em revisão"),
    APPROVED("Aprovada"),
    REVISION_REQUIRED("Revisão solicitada");

    private final String rotulo;

    EntregaStatus(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public boolean permiteTransicaoPara(EntregaStatus destino) {
        return switch (this) {
            case PENDING, REVISION_REQUIRED -> destino == SUBMITTED;
            case SUBMITTED -> destino == UNDER_REVIEW || destino == APPROVED || destino == REVISION_REQUIRED;
            case UNDER_REVIEW -> destino == APPROVED || destino == REVISION_REQUIRED;
            case APPROVED -> false;
        };
    }

    public boolean aguardandoCliente() {
        return this == SUBMITTED || this == UNDER_REVIEW;
    }

    public boolean aguardandoPrestador() {
        return this == PENDING || this == REVISION_REQUIRED;
    }

    @Override
    public String rotulo() {
        return rotulo;
    }
}
