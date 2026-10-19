package com.taskbridge.exception;

import com.taskbridge.model.enums.Estado;

public class TransicaoEstadoInvalidaException extends TaskBridgeException {

    public TransicaoEstadoInvalidaException(Estado<?> origem, Estado<?> destino) {
        super("Operação não permitida: não é possível passar de '%s' para '%s'."
                .formatted(origem.rotulo(), destino.rotulo()));
    }
}
