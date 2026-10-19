package com.taskbridge.model.enums;

import com.taskbridge.exception.TransicaoEstadoInvalidaException;

/**
 * Contrato comum dos enums de status. Cada enum declara as transições permitidas;
 * as entidades só mudam de estado passando por {@link #transicionarPara}.
 */
public interface Estado<E extends Enum<E> & Estado<E>> {

    boolean permiteTransicaoPara(E destino);

    String rotulo();

    default E transicionarPara(E destino) {
        if (!permiteTransicaoPara(destino)) {
            throw new TransicaoEstadoInvalidaException(this, destino);
        }
        return destino;
    }
}
