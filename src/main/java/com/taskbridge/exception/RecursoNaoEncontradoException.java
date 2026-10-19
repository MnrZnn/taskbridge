package com.taskbridge.exception;

/**
 * Também é usada quando o recurso existe mas o usuário não pode vê-lo:
 * responder 403 nesse caso confirmaria a existência do id para quem está enumerando.
 */
public class RecursoNaoEncontradoException extends TaskBridgeException {

    public RecursoNaoEncontradoException(String recurso) {
        super(recurso + " não encontrado(a).");
    }
}
