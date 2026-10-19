package com.taskbridge.model.enums;

import java.util.Locale;

public enum ProvedorLogin {

    GOOGLE("Google"),
    GITHUB("GitHub");

    private final String rotulo;

    ProvedorLogin(String rotulo) {
        this.rotulo = rotulo;
    }

    /** O id de registro no Spring Security é o nome em minúsculas ("google", "github"). */
    public String idRegistro() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ProvedorLogin doIdRegistro(String idRegistro) {
        return valueOf(idRegistro.toUpperCase(Locale.ROOT));
    }

    public String rotulo() {
        return rotulo;
    }
}
