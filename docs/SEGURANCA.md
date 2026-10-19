# Segurança

Revisão final feita sobre o código (item 75 da especificação). Cada linha aponta onde o controle está e qual teste o comprova.

## Checklist

| Tema | Controle implementado | Onde | Teste |
|---|---|---|---|
| SQL Injection | JPA/JPQL parametrizado; busca dinâmica pela Criteria API; curingas do `LIKE` escapados; ordenação nunca vem do cliente | `ProjetoBuscaDao`, `PaginaRequest` | `ValidacaoEntradaIT.injecaoDeSqlNaBuscaNaoAlteraAConsulta`, `ProjetoBuscaDaoTest` |
| XSS (stored/reflected/DOM) | DOM montado com `textContent`/`createElement` (`js/core/dom.js`), nunca `innerHTML`; `href` passa por `linkSeguro` (bloqueia `javascript:`); CSP sem `unsafe-inline`; validação de habilidades por allowlist | `dom.js`, `SecurityConfig` | `AutenticacaoIT.paginasTemCabecalhosDeSeguranca` + verificação manual (título `<b>…</b>` exibido literalmente) |
| CSRF | Ligado. Token guardado na sessão (`HttpSessionCsrfTokenRepository`), entregue em `GET /api/auth/csrf` e exigido no header `X-XSRF-TOKEN`; renovado no login. Sem cookie extra, funciona atrás do Firebase Hosting | `SecurityConfig`, `SessaoAutenticacao` | `AutenticacaoIT.requisicaoQueAlteraEstadoSemTokenCsrfEhRecusada` |
| IDOR / BOLA | Todo service confere dono/participante; privado de outro → 404, ação proibida → 403 | `*Service.carregarDo*` | `AutorizacaoIT` (8 cenários) |
| Broken access control | URL: `/api/**` autenticado; método: `@PreAuthorize` por tipo de conta; domínio: regras de quem pode propor/aceitar | `SecurityConfig`, controllers, `Projeto` | `AutorizacaoIT`, `ProjetoTest` |
| Enumeração de IDs | IDs sequenciais **não** são tratados como segredo; autorização é obrigatória. Perfil público é por `nome_usuario`, não por id | `PerfilPublicoService` | `AutorizacaoIT` |
| Brute force | Rate limit por IP no login (5/min) + bloqueio de 15 min da conta após 5 falhas (pega ataque distribuído) + mensagem única + BCrypt; checagem de bloqueio **depois** do BCrypt para não criar oráculo de tempo | `RateLimitFilter`, `TentativasLoginService`, `SecurityConfig` | `AutenticacaoIT.contaBloqueia…`, `AutenticacaoIT.loginTemRateLimitPorIp` |
| Enumeração de usuários | Login: resposta idêntica para e-mail inexistente, senha errada e conta bloqueada. Cadastro: mensagem única para e-mail **ou** usuário em uso + rate limit (sem e-mail de confirmação não há como esconder totalmente — limitação documentada) | `AuthService` | `AutenticacaoIT.senhaErradaEEmailInexistenteTemAMesmaResposta` |
| Login social (OAuth2/OIDC) | Vínculo pelo id estável do provedor (`sujeito`), não pelo e-mail; conta existente só é vinculada se o provedor garantir e-mail **verificado** (GitHub: consulta `/user/emails`); conta nova exige aceite dos termos; `state` do OAuth e dados pendentes na sessão (expiram em 15 min); erro volta com código fixo, sem refletir dados; contas sem senha não trocam e-mail até definir uma | `LoginSocialService`, `LoginSocialHandler`, `GithubUsuarioService` | `LoginSocialIT`, `LoginSocialConfiguradoIT` |
| Links do perfil | Só `http(s)` com domínio real; recusa `javascript:`/`data:`, URL com usuário embutido (`banco.com@golpe.com`) e endereços locais; no front, `href` atribuído só após validar, com `rel="noopener noreferrer nofollow ugc"`; ícones servidos localmente (sem favicon externo) | `UrlPublica`, `js/core/redes.js` | `LinksPerfilTest`, `PrivacidadeLgpdIT` |
| Rate limiting | Bucket4j: login/cadastro por IP, upload e API por usuário; mapa de buckets com teto (Caffeine) | `RateLimitFilter`, `LimitadorRequisicoes` | `SegurancaUnitariaTest`, `AutenticacaoIT` |
| Upload | Allowlist de extensão + Content-Type + **magic bytes**, extensão ≡ conteúdo, dimensões lidas antes de decodificar, re-encode (remove EXIF/GPS e conteúdo embutido), nome UUID, diretório fora do classpath, só nomes gerados são servidos, `nosniff` | `ProcessadorFotoPerfil`, `ArmazenamentoLocal` | `ProcessadorFotoPerfilTest`, `ValidacaoEntradaIT.uploadInvalidoEhRecusado` |
| Mass assignment | Só DTOs entram; `FAIL_ON_UNKNOWN_PROPERTIES` → `id`, `status`, `clienteId` geram 400 | `application.properties`, `GlobalExceptionHandler` | `ValidacaoEntradaIT.camposQueOClienteNaoControla…`, `usuarioNaoAlteraProprioIdNemStatus` |
| Transições de estado | Nenhum endpoint aceita `status`; mudanças são ações que o enum valida | `model/enums/*Status` | `TransicoesDeEstadoTest`, `FluxoContratacaoIT.statusDoPagamentoNaoPodeSerDefinidoPeloCliente` |
| Segredos | Tudo por variável de ambiente/.env (fora do Git); `.env.example` fictício; sem JWT secret (sessão) | `application*.properties`, `.gitignore` | — |
| Logs | Sem senha/token/e-mail; `toString` de DTOs com senha mascarado; txid mascarado; auditoria só com ids | DTOs `*Request`, `GatewayPagamentoMonitorado` | `GatewayPagamentoTest.txidApareceMascaradoNoLog` |
| Stack trace | `server.error.include-*=never`; handler global devolve só `{status,message,timestamp,path}` | `GlobalExceptionHandler` | `ValidacaoEntradaIT.errosNaoExpoemDetalhesInternos` |
| CORS | Nenhuma origem liberada em produção (mesma origem). Em dev, só as de `CORS_ALLOWED_ORIGINS`; nunca `*` com credenciais | `SecurityConfig.corsConfigurationSource` | — |
| Validação | Jakarta Validation em todos os DTOs + regras de domínio no servidor | `dto/request`, entidades | `ValidacaoEntradaIT` |
| Paginação | Obrigatória com teto de 50 itens; ordenação fixa no servidor | `PaginaRequest` | `ValidacaoEntradaIT.paginacaoTemTeto…` |
| Payload grande | JSON: 64 KB (recusa por Content-Length, ou interrompe a leitura se o corpo vier chunked); upload: 2 MB; proxy: 4 MB | `LimiteCorpoRequisicaoFilter`, `Caddyfile` | `LimiteCorpoRequisicaoFilterTest`, `ValidacaoEntradaIT.corpoGrandeDemais…` |
| Endpoints de debug | `@Profile("!prod")` + propriedade explícita + checagem no service | `PagamentoDebugController`, `PagamentoService.simular` | `PerfilProducaoIT`, `PagamentoServiceTest` |
| Produção | Cookie `Secure`, HSTS no proxy, Swagger desligado, Actuator só `/health` sem detalhes, `forward-headers` só atrás do proxy | `application-prod.properties`, `Caddyfile` | `PerfilProducaoIT`, `AutenticacaoIT.healthCheckNaoExpoeDetalhes` |
| Concorrência | Lock pessimista no aceite + `@Version` + UNIQUE no banco | `PropostaService.aceitar`, V1 | `ConcorrenciaAceiteIT` |
| Headers | CSP restrita a `'self'`, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy`, `Permissions-Policy` | `SecurityConfig` | `AutenticacaoIT.paginasTemCabecalhosDeSeguranca` |

## DDoS: o que a aplicação faz e o que ela não faz

A aplicação **não** é "protegida contra DDoS" — e nenhuma aplicação sozinha é.

**Camada de aplicação (implementado aqui)** — mitiga *abuso*:
- flood de endpoints e de login (rate limit por IP/usuário, resposta 429 com `Retry-After`);
- força bruta (bloqueio temporário da conta);
- requisições caras (paginação com teto, limite de candidatos no matching, corpo e upload limitados);
- recursos presos (timeouts de conexão, transação e query; pool de conexões limitado).

Esses controles consomem CPU e memória do próprio servidor. Diante de um ataque volumétrico (Gbps, milhões de pacotes), o link ou o processo satura antes que qualquer filtro Java rode.

**Camada de infraestrutura (necessária em produção real)** — mitiga *volume*:
- CDN / proxy com proteção DDoS (ex.: Cloudflare, AWS Shield, proteção do provedor de nuvem);
- WAF com regras gerenciadas e rate limit na borda;
- reverse proxy (Caddy/Nginx) com limites de conexão e de corpo;
- load balancer e autoescalonamento;
- firewall liberando só 80/443 no proxy; banco sem IP público.

O rate limit em memória vale por instância. Com várias instâncias, o próximo passo é o Bucket4j com backend compartilhado (JDBC/Redis) ou mover o limite para o proxy/WAF.

## Limitações conhecidas (e aceitas neste escopo)

- Sem verificação de e-mail e sem recuperação de senha (exigiriam infraestrutura de envio de e-mail). A troca de senha e de e-mail exige a senha atual.
- Trocar a senha não derruba outras sessões abertas (sessões ficam em memória; com Spring Session JDBC isso passa a ser possível).
- Os textos de privacidade/termos são acadêmicos e não substituem revisão jurídica.
