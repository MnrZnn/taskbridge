# Publicar com Firebase Hosting + Cloud Run + Cloud SQL

```
seudominio.com ──▶ Firebase Hosting (domínio + HTTPS) ──▶ Cloud Run "taskbridge" ──▶ Cloud SQL (MySQL 8.4)
```

O Firebase Hosting só serve arquivos estáticos; ele repassa todas as requisições (`firebase.json`) para a aplicação
Spring Boot no Cloud Run, na mesma origem. O perfil `firebase` ajusta o que é necessário
(`application-firebase.properties`): cookie de sessão `__session` (o único que o Firebase repassa), cookies `Secure`,
IP real via `X-Forwarded-For` e Swagger desligado.

Requisitos: projeto Firebase no plano **Blaze** e um domínio próprio (opcional).

## 1. Enviar o código para o Cloud Shell

No PowerShell, na pasta do projeto:

```powershell
tar -czf taskbridge-deploy.tar.gz pom.xml Dockerfile .dockerignore src
```

Abra https://console.cloud.google.com, selecione o projeto do Firebase, clique em **Ativar o Cloud Shell** (ícone `>_`),
depois **⋮ › Fazer upload** e envie `taskbridge-deploy.tar.gz`. No Cloud Shell:

```bash
mkdir -p taskbridge && tar -xzf taskbridge-deploy.tar.gz -C taskbridge && cd taskbridge
```

## 2. Variáveis e APIs

```bash
PROJETO=$(gcloud config get project)
REGIAO=southamerica-east1
INSTANCIA=$PROJETO:$REGIAO:taskbridge-db
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com sqladmin.googleapis.com secretmanager.googleapis.com
```

## 3. Banco (Cloud SQL) — leva ~10 minutos

```bash
gcloud sql instances create taskbridge-db --database-version=MYSQL_8_4 --edition=ENTERPRISE --tier=db-f1-micro --region=$REGIAO --storage-size=10GB
gcloud sql databases create taskbridge --instance=taskbridge-db --charset=utf8mb4 --collation=utf8mb4_unicode_ci
read -s -p "Senha do banco: " SENHA_DB; echo
gcloud sql users create taskbridge --instance=taskbridge-db --password="$SENHA_DB"
printf '%s' "$SENHA_DB" | gcloud secrets create taskbridge-db-password --data-file=-
```

## 4. Permissões da conta de serviço do Cloud Run

```bash
SA=$(gcloud projects describe $PROJETO --format='value(projectNumber)')-compute@developer.gserviceaccount.com
gcloud projects add-iam-policy-binding $PROJETO --member=serviceAccount:$SA --role=roles/cloudsql.client
gcloud secrets add-iam-policy-binding taskbridge-db-password --member=serviceAccount:$SA --role=roles/secretmanager.secretAccessor
```

## 5. Publicar a aplicação no Cloud Run

```bash
gcloud run deploy taskbridge --source . --region=$REGIAO --allow-unauthenticated \
  --memory=1Gi --cpu=1 --max-instances=1 \
  --add-cloudsql-instances=$INSTANCIA \
  --set-env-vars="SPRING_PROFILES_ACTIVE=firebase,DB_USER=taskbridge,DB_URL=jdbc:mysql:///taskbridge?cloudSqlInstance=$INSTANCIA&socketFactory=com.google.cloud.sql.mysql.SocketFactory" \
  --set-secrets=DB_PASSWORD=taskbridge-db-password:latest
```

Responda **Y** se perguntar sobre criar o repositório do Artifact Registry. No fim aparece uma URL
`https://taskbridge-….run.app` — abra e confira. As tabelas são criadas pelo Flyway na primeira subida.
Opcional: acrescente `DEMO_DATA=true` às variáveis para criar as contas de demonstração (senha pública — só para apresentação).

## 6. Firebase Hosting (no seu computador)

Na pasta do projeto (o Firebase CLI já está instalado):

```powershell
firebase login
```

```powershell
firebase use --add
```

```powershell
firebase deploy --only hosting
```

Abra `https://SEU-PROJETO.web.app`.

## 7. Domínio próprio

Console do Firebase › **Hosting** › **Adicionar domínio personalizado** › informe o domínio › crie no seu provedor de
DNS os registros **TXT** e **A** mostrados. O certificado HTTPS é emitido automaticamente (de minutos a 24 h).

## 8. Login com Google e GitHub (opcional)

Os botões só aparecem para os provedores configurados. Use **um único endereço** para o site (o domínio próprio):
o provedor devolve o usuário para `APP_BASE_URL`, e a sessão do login precisa estar nesse mesmo endereço.

**Google** — console.cloud.google.com › APIs e serviços:
1. *Tela de permissão OAuth*: tipo **Externo**, nome do app, e-mail de suporte; publique o app.
2. *Credenciais › Criar credenciais › ID do cliente OAuth › Aplicativo da Web*.
   URI de redirecionamento autorizado: `https://SEUDOMINIO/login/oauth2/code/google`.

**GitHub** — github.com › Settings › Developer settings › **OAuth Apps › New OAuth App**:
Homepage `https://SEUDOMINIO`, callback `https://SEUDOMINIO/login/oauth2/code/github`; depois *Generate a new client secret*.

No Cloud Shell (segredos no Secret Manager, IDs como variáveis):

```bash
SA=$(gcloud projects describe $(gcloud config get project) --format='value(projectNumber)')-compute@developer.gserviceaccount.com
for s in google-client-secret github-client-secret; do
  read -s -p "Valor de $s: " V; echo; printf '%s' "$V" | gcloud secrets create $s --data-file=-
  gcloud secrets add-iam-policy-binding $s --member=serviceAccount:$SA --role=roles/secretmanager.secretAccessor
done
gcloud run services update taskbridge --region=southamerica-east1 \
  --update-env-vars=APP_BASE_URL=https://SEUDOMINIO,GOOGLE_CLIENT_ID=SEU_ID_GOOGLE,GITHUB_CLIENT_ID=SEU_ID_GITHUB \
  --update-secrets=GOOGLE_CLIENT_SECRET=google-client-secret:latest,GITHUB_CLIENT_SECRET=github-client-secret:latest
```

Para usar só um dos dois, configure apenas as variáveis dele.

## Atualizar

Gere o zip de novo, faça upload, descompacte e rode só:

```bash
gcloud run deploy taskbridge --source . --region=southamerica-east1
```

Variáveis e segredos são mantidos. O Hosting só precisa de novo `firebase deploy` se o `firebase.json` mudar.

## Custos e limitações

- **Cloud SQL**: cobrado enquanto ligado (~US$ 9–12/mês no `db-f1-micro`). Para pausar:
  `gcloud sql instances patch taskbridge-db --activation-policy=NEVER` (religar com `ALWAYS`).
- **Cloud Run**: `--max-instances=1` e escala a zero; dentro da cota gratuita para pouco tráfego. A primeira
  requisição após ficar ocioso demora ~20 s. `--min-instances=1` evita isso, com custo.
- **Sessões em memória**: um reinício (novo deploy, escala a zero) desconecta os usuários.
- **Fotos de perfil**: o disco do Cloud Run é temporário — as fotos somem a cada reinício. Para guardar de verdade,
  implementar `ArmazenamentoArquivos` com Cloud Storage.
- **PIX**: sempre simulado. O botão "Simular pagamento" fica ativo no perfil `firebase` para que o fluxo possa ser
  demonstrado (só o pagador consegue usá-lo); desligue com `PAYMENT_DEBUG_ENABLED=false`.
- **Rate limit por IP**: atrás do Firebase o IP vem do `X-Forwarded-For`, que o cliente pode forjar; o bloqueio de
  conta continua valendo. Em produção real, somar Cloud Armor.

## Remover tudo

```bash
gcloud run services delete taskbridge --region=southamerica-east1
gcloud sql instances delete taskbridge-db
gcloud secrets delete taskbridge-db-password
```
