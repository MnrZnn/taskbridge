# Diagramas

Todos em Mermaid (renderizam direto no GitHub).

## Arquitetura

```mermaid
flowchart TB
    usuario([Navegador])
    subgraph borda[Borda — infraestrutura]
        cdn[CDN / WAF<br/><i>DDoS volumétrico</i>]
        proxy[Reverse proxy Caddy<br/>TLS · HSTS · limite de corpo]
    end
    subgraph app[Spring Boot — monólito modular]
        direction TB
        filtros[Filtros<br/>tamanho do corpo → contexto de segurança → rate limit → CSRF → autorização]
        estatico[Front-end estático<br/>HTML · CSS · JS · Bootstrap · Lucide]
        controllers[Controllers REST]
        services[Services<br/>casos de uso · transações · dono do recurso]
        dominio[Domínio<br/>entidades · estados · matching]
        persistencia[Repositories + DAO]
        pagamento[Gateway PIX<br/>Factory → Decorator → Mock]
        storage[Storage de fotos]
    end
    mysql[(MySQL 8)]
    disco[(Volume de uploads)]

    usuario -->|HTTPS| cdn --> proxy --> filtros
    filtros --> estatico
    filtros --> controllers --> services --> dominio
    services --> persistencia --> mysql
    services --> pagamento
    services --> storage --> disco
```

## Classes do domínio

```mermaid
classDiagram
    direction LR
    class Usuario {
        -nomeCompleto
        -nomeUsuario
        -email
        -senhaHash
        -tipoConta: TipoConta
        -status: UsuarioStatus
        +registrarFalhaLogin()
        +estaBloqueado(agora) bool
        +anonimizar(hash)
    }
    class PerfilProfissional {
        -tituloProfissional
        -experienciaAnos
        -disponibilidade
        -faixaPreco: FaixaValor
        -habilidades: Set
        +disponivelParaMatching() bool
    }
    class FaixaValor {
        <<value object>>
        -minimo
        -maximo
        +sobrepoe(outra) bool
    }
    class Categoria
    class Servico {
        -titulo
        -precoBase
        -prazoDias
        +pertenceA(id) bool
    }
    class Projeto {
        -titulo
        -necessidade
        -orcamento: FaixaValor
        -prazoDias
        -status: ProjetoStatus
        +pendenciasParaPublicacao() List
        +publicar(agora)
        +receberProposta(prestador, valor, prazo, msg) Proposta
        +aceitarProposta(proposta, agora) Contratacao
    }
    class EscopoProjeto {
        <<builder>>
    }
    class Requisito {
        -tipo: TipoRequisito
        -descricao
    }
    class Proposta {
        -valor
        -prazoDias
        -status: PropostaStatus
        +rejeitar(agora)
        +cancelar(agora)
    }
    class Contratacao {
        <<aggregate root>>
        -valor
        -status: ContratacaoStatus
        +confirmarPagamento(agora)
        +enviarEntrega(id, conteudo, agora)
        +aprovarEntrega(id, agora)
        +temParticipante(id) bool
    }
    class Entrega {
        -titulo
        -status: EntregaStatus
    }
    class Pagamento {
        -txid
        -status: PagamentoStatus
        +confirmar(agora)
        +expirar()
    }

    Usuario "1" -- "0..1" PerfilProfissional
    Usuario "1" -- "*" Servico : prestador
    Usuario "1" -- "*" Projeto : cliente
    Categoria "1" -- "*" Servico
    Categoria "1" -- "*" Projeto
    Projeto "1" *-- "*" Requisito
    Projeto "1" -- "*" Proposta
    Projeto "1" -- "0..1" Contratacao
    Proposta "1" -- "0..1" Contratacao
    Contratacao "1" *-- "*" Entrega
    Contratacao "1" -- "*" Pagamento
    Projeto ..> EscopoProjeto : construído a partir de
    PerfilProfissional *-- FaixaValor
    Projeto *-- FaixaValor
```

## Strategy do matching

```mermaid
classDiagram
    class CriterioCompatibilidade {
        <<interface>>
        +nome() String
        +avaliar(Projeto, CandidatoProfissional) AvaliacaoCriterio
    }
    class CalculadoraCompatibilidade {
        -criterios: List~CriterioPonderado~
        +calcular(Projeto, CandidatoProfissional) ResultadoMatching
    }
    CriterioCompatibilidade <|.. CompatibilidadeCategoria
    CriterioCompatibilidade <|.. CompatibilidadeHabilidades
    CriterioCompatibilidade <|.. CompatibilidadeOrcamento
    CriterioCompatibilidade <|.. CompatibilidadePrazo
    CriterioCompatibilidade <|.. CompatibilidadeDisponibilidade
    CalculadoraCompatibilidade o-- CriterioCompatibilidade : pesos configuráveis
```

## Sequência — fluxo principal

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    actor P as Prestador
    participant API as API (Spring)
    participant DB as MySQL

    C->>API: POST /api/projetos (rascunho)
    C->>API: PUT /api/projetos/{id} (escopo, a cada etapa)
    C->>API: POST /api/projetos/{id}/publicar
    API->>API: Projeto.pendenciasParaPublicacao()
    API-->>C: 200 PUBLISHED (ou 422 com pendências)
    C->>API: GET /api/projetos/{id}/profissionais-compativeis
    API-->>C: pontuação + explicação por critério
    P->>API: POST /api/projetos/{id}/propostas
    API-->>P: 201 PENDING (projeto → IN_NEGOTIATION)
    C->>API: POST /api/propostas/{id}/aceitar
    activate API
    API->>DB: SELECT projeto FOR UPDATE
    API->>API: Projeto.aceitarProposta() → Contratacao + Entregas
    API->>DB: INSERT contratacao, entregas / UPDATE propostas (demais → REJECTED)
    API-->>C: 201 AWAITING_PAYMENT
    deactivate API
    Note over C,API: pagamento PIX (diagrama abaixo)
    P->>API: POST .../entregas/{id}/enviar
    C->>API: POST .../entregas/{id}/solicitar-revisao
    P->>API: POST .../entregas/{id}/enviar
    C->>API: POST .../entregas/{id}/aprovar
    API->>API: todas aprovadas → Contratação COMPLETED, Projeto COMPLETED
    API-->>C: 200 COMPLETED
```

## Sequência — PIX mock

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant API as PagamentoService
    participant GW as GatewayPagamentoMonitorado
    participant M as MockPixGateway
    participant DB as MySQL
    participant J as Agendador

    C->>API: POST /api/contratacoes/{id}/pagamentos
    API->>DB: cobrança vigente? (idempotência)
    API->>GW: criarCobranca(txid, valor)
    GW->>M: criarCobranca(...)
    M-->>GW: BR Code (chave pix-teste@taskbridge.invalid)
    GW-->>API: CobrancaPix (log com txid mascarado)
    API->>DB: INSERT pagamento PENDING
    API-->>C: 201 {qrCode, pixCopiaECola, expiraEm, ambienteTeste: true}

    alt desenvolvimento (perfil ≠ prod e PAYMENT_DEBUG_ENABLED)
        C->>API: POST /api/debug/pagamentos/{id}/simular-pagamento
        API->>DB: SELECT pagamento FOR UPDATE
        API->>API: Pagamento.confirmar() → Contratacao.confirmarPagamento()
        API-->>C: 200 PAID (contratação IN_PROGRESS)
    else produção (adapter real — não implementado)
        Note over API: webhook autenticado do PSP chamaria PagamentoService.confirmar()
    end

    J->>DB: a cada minuto: cobranças PENDING vencidas
    J->>API: expirar(id) → EXPIRED
```

## Estados

```mermaid
stateDiagram-v2
    direction LR
    [*] --> DRAFT
    DRAFT --> PUBLISHED: publicar (escopo completo)
    DRAFT --> CANCELLED
    PUBLISHED --> IN_NEGOTIATION: 1ª proposta
    PUBLISHED --> CONTRACTED: aceite
    IN_NEGOTIATION --> CONTRACTED: aceite
    PUBLISHED --> CANCELLED
    IN_NEGOTIATION --> CANCELLED
    CONTRACTED --> IN_PROGRESS: PIX pago
    IN_PROGRESS --> COMPLETED: todas as entregas aprovadas
```

```mermaid
stateDiagram-v2
    direction LR
    [*] --> PENDING
    PENDING --> SUBMITTED: prestador envia
    SUBMITTED --> UNDER_REVIEW: cliente inicia revisão
    SUBMITTED --> APPROVED
    UNDER_REVIEW --> APPROVED
    SUBMITTED --> REVISION_REQUIRED
    UNDER_REVIEW --> REVISION_REQUIRED
    REVISION_REQUIRED --> SUBMITTED: reenvio
    APPROVED --> [*]
```
