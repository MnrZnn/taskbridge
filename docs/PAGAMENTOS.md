# Pagamentos PIX

## Modo MOCK (padrão)

Nenhum dinheiro real circula. O `MockPixGateway`:

- gera um BR Code no formato EMV real (campos 00–63, CRC16-CCITT), para o QR ser lido por leitores de teste;
- usa sempre a chave `pix-teste@taskbridge.invalid`. O domínio `.invalid` é reservado (RFC 2606) e não recebe e-mail, então não pode ser uma chave registrada no DICT: um app de banco não consegue concluir o pagamento. A chave é fixa no código, não vem de configuração, para que ninguém coloque uma chave real no modo de teste por engano;
- nomeia o recebedor como `TASKBRIDGE TESTE` e a interface mostra a faixa "AMBIENTE DE TESTE — NÃO PAGUE".

Ciclo de vida: `CREATED → PENDING → PAID | EXPIRED | CANCELLED`. A cobrança vale 30 minutos (`taskbridge.pagamento.validade-cobranca`); um agendador expira as vencidas a cada minuto.

### Simulação (somente desenvolvimento)

Com o perfil diferente de `prod` e `PAYMENT_DEBUG_ENABLED=true`, a página de pagamento mostra os botões abaixo e a API expõe:

| Endpoint | Efeito |
|---|---|
| `POST /api/debug/pagamentos/{id}/simular-pagamento` | confirma: contratação vai para `IN_PROGRESS` |
| `POST /api/debug/pagamentos/{id}/simular-expiracao` | expira a cobrança |
| `POST /api/debug/pagamentos/{id}/simular-cancelamento` | cancela a cobrança |

Três travas independentes: o controller não existe no perfil `prod`; precisa da propriedade ligada explicitamente; e `PagamentoService.simular` checa de novo o modo MOCK, o debug e se quem chama é o pagador.

O cliente nunca envia `status`. Não existe `PUT/PATCH` de pagamento (retorna 405).

## Modo REAL (ponto de extensão)

Este projeto **não** integra com nenhum banco ou PSP. Selecionar `PAYMENT_MODE=REAL` faz a aplicação recusar a subida com uma mensagem explicando o porquê, em vez de fingir uma integração.

Para integrar de verdade:

1. Escolha um PSP com API Pix (a especificação do BACEN define os recursos `/cob` e `/cobv`; autenticação costuma ser mTLS + OAuth2 client credentials).
2. Crie uma classe que implemente `GatewayPagamento` (`criarCobranca`, `cancelarCobranca`, `modo`) usando a API do PSP. Credenciais, certificados e a chave Pix entram por variável de ambiente (`PIX_KEY`, etc.) — nunca no código.
3. Devolva essa classe em `GatewayPagamentoFactory.criarGatewayReal()`. Ela é automaticamente envolvida pelo `GatewayPagamentoMonitorado` (log + tradução de falhas).
4. Crie um endpoint de webhook que **valide a autenticidade** da notificação (mTLS/assinatura do PSP), localize o pagamento pelo `txid` e chame `PagamentoService.confirmar(id)` — o mesmo caminho que a simulação usa hoje.
5. Não armazene dados bancários do pagador: o `txid` e o status bastam.
