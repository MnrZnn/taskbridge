package com.taskbridge.exception;

public class PagamentoIndisponivelException extends TaskBridgeException {

    public PagamentoIndisponivelException() {
        super("O serviço de pagamento está indisponível no momento. Tente novamente em instantes.");
    }
}
