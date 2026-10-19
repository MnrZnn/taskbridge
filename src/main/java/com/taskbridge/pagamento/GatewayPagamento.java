package com.taskbridge.pagamento;

import com.taskbridge.model.enums.ModoPagamento;

import java.math.BigDecimal;

/**
 * Porta para o provedor PIX (PSP). O resto da aplicação não sabe se está falando com o mock
 * ou com um banco: só pede uma cobrança e recebe o "copia e cola".
 * <p>
 * A confirmação do pagamento não passa por aqui: num provedor real ela chega por webhook,
 * que deve chamar {@code PagamentoService.confirmar} depois de validar a assinatura do provedor.
 */
public interface GatewayPagamento {

    CobrancaPix criarCobranca(SolicitacaoCobranca solicitacao);

    void cancelarCobranca(String txid);

    ModoPagamento modo();

    record SolicitacaoCobranca(String txid, BigDecimal valor, String descricao) {
    }

    record CobrancaPix(String txid, String payloadCopiaECola) {
    }
}
