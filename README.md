# TaskBridge

Plataforma de contratação de freelancers. O cliente descreve a necessidade, o sistema monta o escopo e sugere profissionais, o freelancer envia proposta, e o projeto segue até a conclusão:

**Necessidade → Escopo → Matching → Proposta → Contratação → Pagamento PIX → Entregas → Aceite → Conclusão**

## Tecnologias

| Área | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 4 (Web, Data JPA, Security, Validation, OAuth2 Client) |
| Banco de dados | MySQL 8 / MariaDB, com migrações Flyway; H2 nos testes |
| Persistência | JPA / Hibernate |
| Front-end | HTML, CSS e JavaScript puro, Bootstrap 5 e ícones Lucide |
| API | REST com JSON, documentada com Swagger (springdoc) |
| Pagamento | PIX simulado com QR Code (ZXing) |
| Rate limiting | Bucket4j + Caffeine |
| Testes | JUnit 5, Spring Boot Test, MockMvc |
| Infraestrutura | Docker, GitHub Actions (CI), Firebase Hosting + Cloud Run + Cloud SQL |

## Arquitetura (MVC em camadas)

```
src/main/java/com/taskbridge/
├── model/        entidades, enums de estado e algoritmo de matching   (Model)
├── controller/   endpoints REST                                       (Controller)
├── service/      regras de negócio
├── repository/   acesso ao banco (Spring Data)
├── dao/          consultas dinâmicas (Criteria API)
├── dto/          dados de entrada e saída da API
├── security/     autenticação, login social, rate limit
├── pagamento/    gateway PIX
├── storage/      upload de fotos
├── config/       configurações
└── exception/    tratamento de erros

src/main/resources/static/
├── view/         páginas HTML                                         (View)
├── js/           scripts das páginas
└── css/          estilos
```

## Implementações

**Padrões de projeto**
- **Strategy**: critérios de matching (habilidades, avaliação, disponibilidade etc.), cada um com seu peso.
- **Factory Method**: escolhe o gateway de pagamento (simulado ou real).
- **Decorator**: adiciona log e tratamento de erros ao gateway de pagamento.
- **Builder**: monta o escopo do projeto e o código PIX.
- **DAO**: busca de projetos com filtros dinâmicos.
- **Máquina de estados**: os enums de status definem quais transições são permitidas.

**Segurança (OWASP)**
- Login com sessão (cookie HttpOnly) e senhas com BCrypt.
- Login com Google e GitHub (OAuth2).
- Proteção CSRF, CORS restrito, CSP e cabeçalhos de segurança.
- Rate limit e bloqueio de conta contra força bruta.
- Verificação de dono em cada recurso (contra IDOR).
- Upload validado pelo conteúdo real do arquivo; a imagem é reprocessada para remover metadados.
- Consultas parametrizadas (contra SQL Injection) e DOM montado sem `innerHTML` (contra XSS).
- Lock no banco para impedir que duas propostas sejam aceitas ao mesmo tempo.

**LGPD**
- Aceite de termos e exportação dos dados do usuário.
- Exclusão de conta com anonimização.
- Logs de auditoria sem dados pessoais.

**Pagamento PIX**
- Totalmente simulado: gera um QR Code válido com uma chave fictícia, então nenhum dinheiro real circula.
- Os botões de simulação existem só fora de produção.

**Testes**
- 146 testes automatizados cobrindo regras de negócio, segurança, fluxo completo de contratação e concorrência.
- Rodam a cada push pelo GitHub Actions.

## Como rodar

Requisitos: JDK 25+, Maven e MySQL/MariaDB (por exemplo, o do XAMPP).

1. Crie o banco:
   ```sql
   CREATE DATABASE taskbridge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'taskbridge'@'localhost' IDENTIFIED BY 'taskbridge-dev';
   GRANT ALL ON taskbridge.* TO 'taskbridge'@'localhost';
   ```
2. Suba a aplicação (no PowerShell):
   ```powershell
   $env:SPRING_PROFILES_ACTIVE="dev,mariadb"; mvn spring-boot:run
   ```
   Com MySQL, use só `dev`.
3. Acesse http://localhost:8080. Contas de demonstração (senha `demo12345678`):
   - `ana@taskbridge.test` (cliente)
   - `joao@taskbridge.test` (freelancer)
   - `marina@taskbridge.test` (freelancer)

Para rodar os testes, use `mvn test`.

## Documentação

- [Arquitetura](docs/ARQUITETURA.md)
- [Diagramas](docs/DIAGRAMAS.md)
- [API](docs/API.md)
- [Segurança](docs/SEGURANCA.md)
- [Pagamentos](docs/PAGAMENTOS.md)
- [Deploy](docs/DEPLOY.md)
- [Firebase](docs/FIREBASE.md)
