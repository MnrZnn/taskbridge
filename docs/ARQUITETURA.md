# TaskBridge — Arquitetura

Este documento registra as decisões tomadas **antes** da implementação (item 83 da especificação) e é mantido atualizado com o código. Diagramas estão em [DIAGRAMAS.md](DIAGRAMAS.md).

## 1. Arquitetura proposta

Monólito modular em camadas, uma única aplicação Spring Boot que expõe a API REST **e** serve o front-end estático (HTML/CSS/JS). Servir tudo na mesma origem é o que permite usar sessão com cookie `HttpOnly` + CSRF em vez de guardar tokens em JavaScript (ver §6 e §8).

```
Navegador ──HTTPS──▶ Reverse proxy (TLS, WAF/CDN opcional)
                        │
                        ▼
              ┌──────────────────────────────────────────┐
              │ Spring Boot                               │
              │  filtros: tamanho de corpo → segurança →  │
              │           rate limit → CSRF → autorização │
              │  controller → service → model             │
              │                  │                        │
              │        repository / dao (JPA)             │
              │  storage (fotos)   pagamento (gateway PIX)│
              └──────────────┬───────────────────────────┘
                             ▼
                          MySQL 8
```

| Camada | Responsabilidade | Não faz |
|---|---|---|
| `controller` | HTTP: bind, `@Valid`, status code, delega | regra de negócio |
| `service` | casos de uso, transação, autorização contextual (dono do recurso), orquestração | SQL, HTTP |
| `model` | entidades com comportamento: transições de estado, invariantes | acesso a banco |
| `model.matching` | Strategy de compatibilidade (Java puro, testável sem Spring) | — |
| `repository` / `dao` | persistência | regra de negócio |
| `pagamento` | abstração do gateway PIX, BR Code e QR Code | decidir quem pode pagar |
| `storage` | gravação/leitura de arquivos validados | — |
| `security` | autenticação, sessão, rate limit, filtros | — |

## 2. Estrutura de pastas

```
src/main/java/com/taskbridge/
├── config/            SecurityConfig, WebConfig, OpenApiConfig, *Properties
├── controller/        um controller por recurso REST + PaginasController
├── service/           ProjetoService, PropostaService, ContratacaoService, ...
├── repository/        interfaces Spring Data (DAO gerado pelo Spring)
├── dao/               ProjetoBuscaDao — busca dinâmica com Criteria API
├── model/             MODEL do MVC: entidades + value objects (FaixaValor)
│   ├── enums/         estados com transições controladas
│   └── matching/      CriterioCompatibilidade e implementações
├── pagamento/         GatewayPagamento, MockPixGateway, decorator, factory, BrCodePix
├── storage/           ArmazenamentoArquivos, ArmazenamentoLocal, ProcessadorFotoPerfil
├── security/          UsuarioAutenticado, RateLimitFilter, LimiteCorpoRequisicaoFilter...
├── dto/request|response   records imutáveis
└── exception/         exceções de domínio + GlobalExceptionHandler
```

Pastas da sugestão original que **não** foram criadas:

- `mapper/` — o mapeamento entidade → DTO é feito por métodos estáticos `de(...)` nos próprios records de resposta. Uma classe mapper por entidade só duplicaria a lista de campos.
- `util/` — nenhum código se encaixou como "utilitário" genérico; cada helper pertence a um pacote específico.
- `rules/` — as regras ficaram dentro das entidades e dos enums de estado, que são os donos naturais delas.

## 3. Modelo de dados

| Tabela | Finalidade | Destaques |
|---|---|---|
| `usuario` | conta | `email` e `nome_usuario` UNIQUE, `senha_hash` BCrypt, controle de bloqueio de login, versão dos termos aceitos |
| `perfil_profissional` | dados profissionais do prestador | `usuario_id` UNIQUE (1:0..1), faixa de preço com CHECK |
| `perfil_habilidade` | habilidades do perfil | PK composta |
| `categoria` | categorias de serviço | `slug` UNIQUE, `sigla` usada na identidade visual |
| `servico` | serviço ofertado por prestador | índice `(categoria_id, status)` usado pelo matching |
| `projeto` | necessidade do cliente estruturada | `version` (lock otimista), índices por status/categoria e cliente |
| `projeto_habilidade` | habilidades desejadas | PK composta |
| `requisito` | requisitos, entregáveis e critérios de aceite | `tipo` distingue os três |
| `proposta` | proposta de um prestador | UNIQUE `(projeto_id, prestador_id)` |
| `contratacao` | vínculo após aceite | `projeto_id` UNIQUE e `proposta_id` UNIQUE |
| `entrega` | entregáveis da contratação | geradas a partir dos requisitos `ENTREGAVEL` |
| `pagamento` | cobrança PIX | `txid` UNIQUE, índice `(status, expira_em)` para expiração |
| `notificacao` | caixa de entrada do sistema | índice `(usuario_id, lida)` |
| `registro_auditoria` | eventos de segurança e negócio | sem FK para usuário: sobrevive à anonimização |

Decisões:

- IDs `BIGINT AUTO_INCREMENT`. **Não** usamos UUID como mecanismo de segurança: todo acesso passa por verificação de propriedade. O perfil público usa `nome_usuario` na URL (e não o id) para não permitir varrer usuários por número. O `txid` do PIX é aleatório porque o padrão do BR Code exige.
- Estados como `VARCHAR` + `CHECK`, não `ENUM` nativo: adicionar um estado vira uma migration simples.
- Nenhum `TEXT`/`BLOB`: textos têm limite explícito e a foto fica no disco (só o nome do arquivo no banco).
- Não coletamos CPF, telefone, endereço ou data de nascimento — não há finalidade para eles no fluxo.

## 4. Relacionamentos

```
Usuario 1 ── 0..1 PerfilProfissional
Usuario 1 ── N Projeto            (cliente)
Usuario 1 ── N Servico            (prestador)
Usuario 1 ── N Proposta           (prestador)
Categoria 1 ── N Servico
Categoria 1 ── N Projeto
Projeto 1 ── N Requisito
Projeto 1 ── N Proposta
Projeto 1 ── 0..1 Contratacao
Proposta 1 ── 0..1 Contratacao
Contratacao 1 ── N Entrega
Contratacao 1 ── N Pagamento      (um ativo por vez; expirados ficam no histórico)
```

## 5. Regras de negócio (onde estão implementadas)

| # | Regra | Onde |
|---|---|---|
| 1 | Usuário não envia proposta ao próprio projeto | `Projeto.receberProposta` |
| 2 | Só o dono do projeto aceita proposta | `PropostaService.aceitar` + `Projeto.pertenceA` |
| 3 | Proposta rejeitada não pode ser aceita | `PropostaStatus` (transições) |
| 4 | Proposta aceita não é aceita de novo | `PropostaStatus` + UNIQUE `contratacao.proposta_id` |
| 5 | Uma contratação por projeto | `ProjetoStatus` + UNIQUE `contratacao.projeto_id` + lock pessimista |
| 6 | Entrega só é aceita depois de enviada | `EntregaStatus` |
| 7 | Entrega aprovada não volta para pendente | `EntregaStatus.APPROVED` é terminal |
| 8 | Só participantes veem a contratação | `Contratacao.temParticipante` → 404 para terceiros |
| 9 | Ninguém edita recurso de outro | `pertenceA(...)` em Projeto, Servico, Proposta |
| 10 | Projeto precisa de informações mínimas para publicar | `Projeto.pendenciasParaPublicacao` |

Nenhum endpoint recebe `status`: toda mudança de estado é uma ação (`/publicar`, `/aceitar`, `/aprovar`...) que chama um método do domínio, e o enum decide se a transição é válida.

## 6. Estratégia de segurança

| Ameaça | Controle |
|---|---|
| SQL Injection | Spring Data derivado, JPQL com parâmetros nomeados, Criteria API no `ProjetoBuscaDao`. Nenhuma concatenação de SQL. Ordenação não aceita campo vindo do cliente. |
| XSS (stored/reflected/DOM) | Front-end monta DOM com `textContent`/`createElement`, nunca `innerHTML` com dado de usuário. CSP `script-src 'self'` sem `unsafe-inline`. Respostas da API são JSON. |
| CSRF | Sessão em cookie ⇒ CSRF **ligado**: token guardado na sessão, entregue por `GET /api/auth/csrf` e exigido no header `X-XSRF-TOKEN`; cookie de sessão `SameSite=Lax`. |
| IDOR/BOLA | Verificação de propriedade no service para todo recurso. Recurso privado de outro usuário responde 404 (não confirma existência); ação proibida em recurso visível responde 403. |
| Mass assignment | Só DTOs entram; `FAIL_ON_UNKNOWN_PROPERTIES` ligado — enviar `id`, `status`, `clienteId` gera 400. |
| Brute force | Rate limit por IP no login + bloqueio temporário da conta após N falhas + mensagem genérica + BCrypt (que já é lento). |
| Abuso / DoS de aplicação | Bucket4j por IP e por usuário, limite de corpo JSON, limite de upload, paginação com teto, timeouts de conexão/transação/query. |
| DoS volumétrico | **Fora do alcance da aplicação.** Depende de CDN/WAF/proxy/provedor (ver README). |
| Upload | Allowlist de tipo, verificação de assinatura (magic bytes), limite de dimensões antes de decodificar, re-encode da imagem (remove EXIF e conteúdo embutido), nome aleatório, diretório fora do classpath, `nosniff`. |
| Segredos | Tudo via variável de ambiente; `.env` no `.gitignore`; `.env.example` só com valores fictícios. |
| Vazamento de erro | Handler global devolve `{status, message, timestamp, path}`; stack trace nunca vai para o cliente. |
| Endpoints de debug | Existem só com perfil ≠ `prod` **e** `PAYMENT_DEBUG_ENABLED=true` **e** modo MOCK; em `prod` o bean nem é criado. |
| CORS | Front-end é same-origin, então em produção nenhuma origem extra é liberada. Em dev, só as origens listadas em `CORS_ALLOWED_ORIGINS`. |

## 7. Design Patterns

| Padrão | Onde | Por que existe |
|---|---|---|
| **MVC** | controllers REST + páginas estáticas + domínio | separar HTTP, regra e apresentação |
| **DAO** | `repository/*` (Spring Data) e `dao/ProjetoBuscaDao` | acesso a dados isolado; o DAO manual existe só para a busca com filtros opcionais, que não cabe em método derivado |
| **Strategy** | `CriterioCompatibilidade` + 5 critérios | critérios de matching evoluem independentemente; novo critério = nova classe (OCP) |
| **Builder** | `Projeto.Builder`, `BrCodePix.Builder` | projeto tem muitos campos opcionais no rascunho; o BR Code tem campos EMV opcionais e precisa calcular CRC no final |
| **Factory Method** | `GatewayPagamentoFactory` | escolhe a implementação de gateway (MOCK/REAL) a partir da configuração e já devolve o objeto decorado |
| **Decorator** | `GatewayPagamentoMonitorado` | acrescenta a qualquer gateway (mock ou real) log de duração/resultado com txid mascarado e traduz falhas técnicas do provedor em `PagamentoIndisponivelException` (503), sem alterar as implementações |

Padrões avaliados e **não** usados: Factory para notificações (um enum com template resolve), Builder em entidades simples, interfaces para todos os services (só existe interface onde há mais de uma implementação: gateway, storage, critério).

## 8. Fluxo de autenticação

Sessão server-side, não JWT. Motivo: o front-end é servido pela mesma origem, então um cookie `HttpOnly` é inacessível a JavaScript (um XSS não consegue roubar a sessão), logout invalida de fato no servidor e não há segredo de assinatura para gerenciar. O custo é precisar de CSRF — que foi implementado, não desligado.

```
GET  /api/auth/csrf       → devolve o token CSRF (guardado na sessão; o front o mantém só em memória)
POST /api/auth/login      → header X-XSRF-TOKEN + {email, senha}
     ├─ rate limit por IP
     ├─ conta bloqueada? → 401 genérico
     ├─ BCrypt confere?  → não: incrementa falhas, audita, 401 genérico
     └─ sim: novo ID de sessão (anti fixation), novo token CSRF, audita
Requisições seguintes: cookie de sessão (TBSESSION, ou __session no Firebase; HttpOnly, SameSite=Lax, Secure em prod) + X-XSRF-TOKEN
POST /api/auth/logout     → invalida sessão, apaga cookie, audita
```

Para escalar horizontalmente basta adicionar Spring Session JDBC (a sessão passa a ficar no MySQL); nada no código de domínio muda.

## 9. Estratégia PIX Mock

- `GatewayPagamento` (interface) com `MockPixGateway`. O modo REAL é um ponto de extensão documentado: selecionar `PAYMENT_MODE=REAL` sem implementar um adapter para um PSP faz a aplicação **falhar na inicialização** com mensagem clara, em vez de fingir uma integração.
- O mock gera um BR Code no formato EMV real (com CRC16), mas com chave `pix-teste@taskbridge.invalid` (domínio `.invalid` não recebe e-mail, portanto não pode ser uma chave DICT registrada), recebedor `TASKBRIDGE TESTE` e a interface mostra "AMBIENTE DE TESTE — NÃO PAGUE".
- Estados: `CREATED → PENDING → PAID | EXPIRED | CANCELLED`.
- Ninguém envia `status`. Pagamento confirmado só via `PagamentoService.confirmar`, o mesmo caminho que um webhook real usaria. Em dev, o `PagamentoDebugController` chama esse caminho; em prod ele não existe.
- Expiração automática por tarefa agendada.

## 10. Estratégia de deploy

- Imagem Docker multi-stage (build com JDK, runtime com JRE, usuário não-root).
- `docker-compose.yml` para dev: MySQL + aplicação.
- Produção: reverse proxy (Nginx/Caddy ou o do provedor) terminando TLS, aplicação em rede privada, MySQL gerenciado sem IP público, backups automáticos do provedor, health check em `/actuator/health`.
- GitHub Actions: build + testes em todo push/PR; imagem publicada no GHCR a partir da `main`; deploy como job manual que depende de secrets configurados no GitHub.

## 11. Fluxo principal do usuário

```
Cadastro → Login → Perfil (prestador: perfil profissional + serviços)
Cliente: Nova necessidade → Escopo → Orçamento/prazo → Requisitos → Revisão → Publicar
Sistema: matching explica a compatibilidade de cada profissional
Prestador: vê projetos compatíveis → envia proposta
Cliente: compara propostas → aceita uma (demais são recusadas na mesma transação)
Contratação criada (aguardando pagamento) → PIX mock → pagamento confirmado → em andamento
Prestador envia entregas → cliente aprova ou pede revisão
Todas aprovadas → contratação e projeto concluídos
```

## 12. Plano de implementação

Seguido na ordem da especificação: projeto Spring Boot → MySQL/Flyway → entidades e relacionamentos → repositories/DAO → services → controllers → segurança → front-end → matching → contratação → PIX mock → testes → hardening → Docker → CI/CD → deploy → documentação.
