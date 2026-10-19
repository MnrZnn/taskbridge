package com.taskbridge.pagamento;

import com.taskbridge.model.enums.ModoPagamento;
import com.taskbridge.exception.PagamentoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Decorator: acrescenta a qualquer gateway (mock ou real) o registro de duração/resultado das chamadas
 * e a tradução de falhas técnicas do provedor numa exceção de domínio. As implementações concretas
 * continuam cuidando só do protocolo do provedor.
 * <p>
 * O txid aparece mascarado no log: é o identificador da cobrança e não precisa estar inteiro ali.
 */
public class GatewayPagamentoMonitorado implements GatewayPagamento {

    private static final Logger log = LoggerFactory.getLogger(GatewayPagamentoMonitorado.class);

    private final GatewayPagamento delegado;

    public GatewayPagamentoMonitorado(GatewayPagamento delegado) {
        this.delegado = delegado;
    }

    @Override
    public CobrancaPix criarCobranca(SolicitacaoCobranca solicitacao) {
        return executar("criarCobranca", solicitacao.txid(), () -> delegado.criarCobranca(solicitacao));
    }

    @Override
    public void cancelarCobranca(String txid) {
        executar("cancelarCobranca", txid, () -> {
            delegado.cancelarCobranca(txid);
            return null;
        });
    }

    @Override
    public ModoPagamento modo() {
        return delegado.modo();
    }

    private <T> T executar(String operacao, String txid, Supplier<T> chamada) {
        long inicio = System.nanoTime();
        try {
            T resultado = chamada.get();
            log.info("gateway={} operacao={} txid={} duracaoMs={}", modo(), operacao, mascarar(txid), duracaoMs(inicio));
            return resultado;
        } catch (RuntimeException e) {
            log.error("gateway={} operacao={} txid={} duracaoMs={} falha={}",
                    modo(), operacao, mascarar(txid), duracaoMs(inicio), e.getClass().getSimpleName());
            throw new PagamentoIndisponivelException();
        }
    }

    private static long duracaoMs(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000;
    }

    static String mascarar(String txid) {
        if (txid == null || txid.length() <= 6) {
            return "***";
        }
        return txid.substring(0, 4) + "…" + txid.substring(txid.length() - 2);
    }
}
