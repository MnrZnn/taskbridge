# Deploy

## Arquitetura de produção

```
Usuário ──HTTPS──▶ (CDN/WAF opcional) ──▶ Caddy :443 ──rede interna──▶ app :8080 ──▶ MySQL :3306
```

- **TLS**: o Caddy obtém e renova o certificado (Let's Encrypt) e envia HSTS.
- **Rede**: só o Caddy publica portas (80/443). Aplicação e banco ficam na rede interna do Docker. No firewall da VM, libere apenas 80, 443 e SSH (de preferência restrito ao seu IP).
- **Banco**: prefira MySQL gerenciado (backups automáticos, patches, réplica). Nesse caso, remova o serviço `mysql` do compose, aponte `DB_HOST` para o endpoint privado e use `DB_SSL_MODE=VERIFY_IDENTITY`.
- **Uploads**: volume `uploads`. Em mais de uma instância, troque `ArmazenamentoLocal` por um storage de objetos (S3/GCS) implementando `ArmazenamentoArquivos`.

## Passo a passo (VM única)

1. Instale Docker no servidor e crie `/opt/taskbridge`.
2. Copie `deploy/docker-compose.prod.yml` e `deploy/Caddyfile` para lá.
3. Crie `/opt/taskbridge/.env` a partir de `.env.example` com valores reais e fortes, mais:
   ```env
   DOMINIO=taskbridge.seudominio.com
   GITHUB_REPOSITORY=sua-org/taskbridge
   ```
   Permissão `600` no arquivo.
4. `docker compose -f docker-compose.prod.yml up -d`
5. Confira: `curl https://taskbridge.seudominio.com/actuator/health` → `{"status":"UP",...}`.

O Flyway aplica as migrations na subida. `ddl-auto=validate` faz a aplicação recusar a subida se o schema divergir das entidades.

## CI/CD (GitHub Actions)

`.github/workflows/ci.yml`:

| Job | Quando | O que faz |
|---|---|---|
| build-e-testes | todo push/PR | `mvn verify` — testes unitários e de integração (H2) |
| migrations-mysql | todo push/PR | sobe um MySQL 8.4 e roda migrations + validação do schema contra ele |
| imagem | push na `main` | publica a imagem no GHCR (`latest` e o SHA do commit) |
| deploy | push na `main` com `vars.DEPLOY_ENABLED == 'true'` | via SSH: `docker compose pull && up -d` |

Configuração no GitHub (nada disso vai para o repositório):

- **Environment** `production`, opcionalmente com aprovação manual obrigatória.
- **Secrets**: `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY` (chave dedicada, só com acesso a `/opt/taskbridge`), `DEPLOY_KNOWN_HOSTS` (fingerprint do servidor — evita MITM no SSH).
- **Variable**: `DEPLOY_ENABLED=true` quando o servidor estiver pronto.

## Operação

- **Health check**: `/actuator/health` (liveness/readiness em `/actuator/health/liveness|readiness`). Não expõe detalhes.
- **Logs**: `docker compose logs -f app`. Eventos de negócio/segurança saem no logger `taskbridge.auditoria` e ficam na tabela `registro_auditoria` (180 dias).
- **Backup**: com MySQL gerenciado, ative backups automáticos + point-in-time recovery. Em VM, agende `mysqldump --single-transaction` diário para um storage externo e **teste a restauração**. Faça backup também do volume `uploads`.
- **Monitoramento básico**: uptime check externo no `/actuator/health`; alerta de disco (uploads e MySQL) e de reinícios do container.
- **Atualização**: o pipeline publica a nova imagem; `up -d` recria só o container da aplicação. As migrations do Flyway são aditivas.
