package com.taskbridge.pagamento;

import com.taskbridge.model.enums.ModoPagamento;

/**
 * Gateway de desenvolvimento. Gera um BR Code com formato válido, mas com chave em domínio
 * {@code .invalid} (reservado pela RFC 2606): esse endereço não recebe e-mail, então não pode ter sido
 * registrado como chave no DICT, e nenhum app de banco consegue concluir um pagamento para ele.
 * <p>
 * A chave é fixa aqui de propósito e não vem de configuração: assim ninguém coloca uma chave real
 * no modo MOCK por engano e recebe dinheiro de verdade durante um teste.
 */
public class MockPixGateway implements GatewayPagamento {

    static final String CHAVE_TESTE = "pix-teste@taskbridge.invalid";
    static final String RECEBEDOR_TESTE = "TASKBRIDGE TESTE";
    static final String CIDADE_TESTE = "SAO PAULO";

    @Override
    public CobrancaPix criarCobranca(SolicitacaoCobranca solicitacao) {
        String payload = BrCodePix.builder()
                .chave(CHAVE_TESTE)
                .valor(solicitacao.valor())
                .nomeRecebedor(RECEBEDOR_TESTE)
                .cidade(CIDADE_TESTE)
                .txid(solicitacao.txid())
                .descricao("TESTE " + solicitacao.descricao())
                .build();
        return new CobrancaPix(solicitacao.txid(), payload);
    }

    @Override
    public void cancelarCobranca(String txid) {
        // Não há cobrança externa a cancelar no mock; o estado vive só no banco da aplicação.
    }

    @Override
    public ModoPagamento modo() {
        return ModoPagamento.MOCK;
    }
}
