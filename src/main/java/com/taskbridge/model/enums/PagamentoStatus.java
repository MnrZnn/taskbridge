package com.taskbridge.model.enums;

public enum PagamentoStatus implements Estado<PagamentoStatus> {

    CREATED("PIX criado"),
    PENDING("Aguardando pagamento"),
    PAID("Pago"),
    EXPIRED("Expirado"),
    CANCELLED("Cancelado");

    private final String rotulo;

    PagamentoStatus(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public boolean permiteTransicaoPara(PagamentoStatus destino) {
        return switch (this) {
            case CREATED -> destino == PENDING || destino == CANCELLED;
            case PENDING -> destino == PAID || destino == EXPIRED || destino == CANCELLED;
            case PAID, EXPIRED, CANCELLED -> false;
        };
    }

    public boolean emAberto() {
        return this == CREATED || this == PENDING;
    }

    @Override
    public String rotulo() {
        return rotulo;
    }
}
