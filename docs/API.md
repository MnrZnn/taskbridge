# API REST

Documentação interativa (perfil `dev`): **http://localhost:8080/swagger-ui.html** — OpenAPI em `/v3/api-docs`. Desligada no perfil `prod`.

## Convenções

- JSON em UTF-8. Campos desconhecidos no corpo → **400** (`"Campo não permitido: x"`).
- Autenticação por sessão: `POST /api/auth/login` cria o cookie de sessão HttpOnly (`TBSESSION`; `__session` no perfil `firebase`).
- **CSRF**: toda requisição `POST/PUT/DELETE` precisa do header `X-XSRF-TOKEN` com o token devolvido por `GET /api/auth/csrf` (o token muda após login/logout).
- Listagens: `?page=0&size=20` (máx. 50). A resposta vem em `{conteudo, pagina, tamanho, totalElementos, totalPaginas}`.
- Erros: `{status, message, timestamp, path}` + `campos` (validação) ou `detalhes` (regra de negócio).

| Código | Quando |
|---|---|
| 200 / 201 / 204 | sucesso / criado (com `Location`) / sem corpo |
| 400 | JSON inválido, validação, campo não permitido, upload inválido |
| 401 | não autenticado ou credenciais inválidas (mensagem única) |
| 403 | autenticado, mas sem permissão (tipo de conta, dono do recurso) ou CSRF ausente |
| 404 | não existe **ou** é privado de outro usuário |
| 405 | método não suportado (ex.: `PUT /api/pagamentos/{id}`) |
| 409 | transição de estado inválida, duplicidade, conflito de concorrência |
| 413 | corpo ou upload acima do limite |
| 422 | regra de negócio não atendida (ex.: publicar projeto incompleto — lista em `detalhes`) |
| 429 | rate limit (com `Retry-After`) |
| 503 | provedor de pagamento indisponível |

## Endpoints

**Legenda:** 🌐 público · 🔒 autenticado · C cliente · P prestador

### Autenticação e conta

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/auth/csrf` | 🌐 | gera o cookie/token CSRF |
| POST | `/api/auth/registro` | 🌐 | cria conta (`aceiteTermos: true` obrigatório) |
| POST | `/api/auth/login` | 🌐 | inicia sessão |
| POST | `/api/auth/logout` | 🔒 | encerra sessão (204) |
| GET / PUT | `/api/me` | 🔒 | dados da própria conta |
| PUT | `/api/me/email`, `/api/me/senha` | 🔒 | exigem `senhaAtual` |
| POST / DELETE | `/api/me/foto` | 🔒 | multipart `arquivo` (JPG/PNG ≤ 2 MB) |
| GET / PUT | `/api/me/perfil-profissional` | 🔒 P | perfil usado no matching |
| GET | `/api/me/eventos-seguranca` | 🔒 | auditoria da própria conta |
| GET | `/api/me/dados` | 🔒 | LGPD: exportação em JSON |
| POST | `/api/me/exclusao` | 🔒 | LGPD: `{senhaAtual, confirmacao: "EXCLUIR"}` |
| GET | `/api/usuarios/{nomeUsuario}` | 🌐 | perfil público |

### Catálogo, serviços e projetos

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/categorias`, `/api/config/publica` | 🌐 | dados de referência |
| GET | `/api/servicos?categoriaId=` | 🌐 | serviços ativos |
| GET | `/api/servicos/meus` | 🔒 P | |
| POST / PUT / DELETE | `/api/servicos[/{id}]` | 🔒 P (dono) | |
| POST | `/api/servicos/{id}/pausar`, `/reativar` | 🔒 P (dono) | |
| GET | `/api/projetos?texto=&categoriaId=&orcamentoMinimo=` | 🌐 | projetos abertos |
| GET | `/api/projetos/meus` | 🔒 C | |
| GET | `/api/projetos/compativeis` | 🔒 P | matching para o prestador |
| GET | `/api/projetos/{id}` | 🌐 / dono | rascunho só para o dono |
| POST / PUT | `/api/projetos[/{id}]` | 🔒 C (dono) | cria/edita rascunho |
| POST | `/api/projetos/{id}/publicar`, `/cancelar` | 🔒 C (dono) | |
| DELETE | `/api/projetos/{id}` | 🔒 C (dono) | só rascunho |
| GET | `/api/projetos/{id}/profissionais-compativeis` | 🔒 C (dono) | matching explicado |
| POST | `/api/projetos/{id}/convites/{nomeUsuario}` | 🔒 C (dono) | notifica o prestador (idempotente) |

### Propostas, contratações e pagamentos

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/projetos/{id}/propostas` | 🔒 P | uma por prestador por projeto |
| GET | `/api/projetos/{id}/propostas` | 🔒 C (dono) | |
| GET | `/api/propostas/enviadas` / `recebidas` | 🔒 P / C | |
| POST | `/api/propostas/{id}/aceitar` | 🔒 C (dono) | 201 com a contratação |
| POST | `/api/propostas/{id}/rejeitar` / `cancelar` | 🔒 C / P | |
| GET | `/api/contratacoes[/{id}]` | 🔒 participantes | |
| POST | `/api/contratacoes/{id}/pagamentos` | 🔒 C | gera cobrança PIX (201) ou devolve a vigente (200) |
| POST | `/api/contratacoes/{id}/entregas/{entregaId}/enviar` | 🔒 P | `{conteudo}` |
| POST | `.../iniciar-revisao`, `.../aprovar` | 🔒 C | |
| POST | `.../solicitar-revisao` | 🔒 C | `{feedback}` |
| GET | `/api/pagamentos/{id}` | 🔒 participantes | QR Code + copia e cola enquanto aberto |
| POST | `/api/debug/pagamentos/{id}/simular-pagamento` (`-expiracao`, `-cancelamento`) | 🔒 C, **só fora de prod** | ver [PAGAMENTOS.md](PAGAMENTOS.md) |

### Painel e notificações

| Método | Rota | Acesso |
|---|---|---|
| GET | `/api/dashboard` | 🔒 |
| GET | `/api/notificacoes`, `/api/notificacoes/nao-lidas` | 🔒 |
| POST | `/api/notificacoes/{id}/lida`, `/api/notificacoes/lidas` | 🔒 |
| GET | `/actuator/health` | 🌐 |
