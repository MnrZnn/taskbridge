package com.taskbridge.exception;

import java.util.List;

public class RegraNegocioException extends TaskBridgeException {

    private final List<String> detalhes;

    public RegraNegocioException(String message) {
        this(message, List.of());
    }

    public RegraNegocioException(String message, List<String> detalhes) {
        super(message);
        this.detalhes = List.copyOf(detalhes);
    }

    public List<String> getDetalhes() {
        return detalhes;
    }
}
