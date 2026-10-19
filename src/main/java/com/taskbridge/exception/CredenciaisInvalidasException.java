package com.taskbridge.exception;

/**
 * Mensagem única para e-mail inexistente, senha errada e conta bloqueada,
 * para que a resposta do login não sirva para descobrir quais e-mails estão cadastrados.
 */
public class CredenciaisInvalidasException extends TaskBridgeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos.");
    }
}
