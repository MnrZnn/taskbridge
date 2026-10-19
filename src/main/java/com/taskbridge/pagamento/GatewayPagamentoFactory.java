package com.taskbridge.pagamento;

import com.taskbridge.model.enums.ModoPagamento;

/**
 * Factory Method: decide qual gateway criar a partir do modo configurado ({@code PAYMENT_MODE})
 * e já devolve o objeto decorado com monitoramento. Quem usa o gateway nunca sabe qual é.
 */
public class GatewayPagamentoFactory {

    public GatewayPagamento criar(ModoPagamento modo) {
        GatewayPagamento base = switch (modo) {
            case MOCK -> new MockPixGateway();
            case REAL -> criarGatewayReal();
        };
        return new GatewayPagamentoMonitorado(base);
    }

    /**
     * Este projeto não integra com nenhum PSP de verdade. Em vez de fingir uma API bancária,
     * a aplicação se recusa a subir. Para integrar: implemente {@link GatewayPagamento} usando a API
     * Pix do PSP escolhido (cob/cobv, autenticação mTLS + OAuth2) e devolva-o aqui.
     */
    private GatewayPagamento criarGatewayReal() {
        throw new IllegalStateException("PAYMENT_MODE=REAL requer um adapter para um provedor PIX (PSP), "
                + "que não faz parte desta implementação acadêmica. Use PAYMENT_MODE=MOCK ou implemente GatewayPagamento. "
                + "Veja docs/PAGAMENTOS.md.");
    }
}
