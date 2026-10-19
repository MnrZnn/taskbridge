package com.taskbridge.model.enums;

public enum TipoConta {

    CLIENTE,
    PRESTADOR;

    public String authority() {
        return "ROLE_" + name();
    }
}
