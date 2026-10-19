package com.taskbridge.model.enums;

public enum Disponibilidade {

    IMEDIATA("Imediata", 0),
    EM_ATE_15_DIAS("Em até 15 dias", 15),
    EM_ATE_30_DIAS("Em até 30 dias", 30),
    INDISPONIVEL("Indisponível", Integer.MAX_VALUE);

    private final String rotulo;
    private final int diasAteInicio;

    Disponibilidade(String rotulo, int diasAteInicio) {
        this.rotulo = rotulo;
        this.diasAteInicio = diasAteInicio;
    }

    public boolean aceitaNovosProjetos() {
        return this != INDISPONIVEL;
    }

    public int diasAteInicio() {
        return diasAteInicio;
    }

    public String rotulo() {
        return rotulo;
    }
}
