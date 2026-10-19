-- Estados são VARCHAR + CHECK (e não ENUM nativo) para que um novo estado seja só uma migration.
-- Nenhuma coluna TEXT/BLOB: todo texto tem limite explícito e fotos ficam fora do banco.

CREATE TABLE usuario (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    nome_completo           VARCHAR(120) NOT NULL,
    nome_usuario            VARCHAR(30)  NOT NULL,
    email                   VARCHAR(160) NOT NULL,
    senha_hash              VARCHAR(100) NOT NULL,
    tipo_conta              VARCHAR(20)  NOT NULL,
    biografia               VARCHAR(1000),
    foto_perfil             VARCHAR(80),
    localizacao             VARCHAR(80),
    status                  VARCHAR(20)  NOT NULL,
    tentativas_login_falhas INT          NOT NULL DEFAULT 0,
    bloqueado_ate           DATETIME(6),
    termos_versao           VARCHAR(20)  NOT NULL,
    termos_aceitos_em       DATETIME(6)  NOT NULL,
    data_cadastro           DATETIME(6)  NOT NULL,
    data_atualizacao        DATETIME(6)  NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_nome_usuario UNIQUE (nome_usuario),
    CONSTRAINT ck_usuario_tipo_conta CHECK (tipo_conta IN ('CLIENTE', 'PRESTADOR')),
    CONSTRAINT ck_usuario_status CHECK (status IN ('ATIVO', 'EXCLUIDO')),
    CONSTRAINT ck_usuario_tentativas CHECK (tentativas_login_falhas >= 0)
);

CREATE TABLE perfil_profissional (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    usuario_id          BIGINT        NOT NULL,
    titulo_profissional VARCHAR(100)  NOT NULL,
    descricao           VARCHAR(2000),
    experiencia_anos    INT           NOT NULL,
    disponibilidade     VARCHAR(20)   NOT NULL,
    modalidade_trabalho VARCHAR(20)   NOT NULL,
    faixa_preco_minima  DECIMAL(12,2) NOT NULL,
    faixa_preco_maxima  DECIMAL(12,2) NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    criado_em           DATETIME(6)   NOT NULL,
    CONSTRAINT pk_perfil_profissional PRIMARY KEY (id),
    CONSTRAINT uk_perfil_profissional_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_perfil_profissional_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_perfil_experiencia CHECK (experiencia_anos BETWEEN 0 AND 60),
    CONSTRAINT ck_perfil_faixa_preco CHECK (faixa_preco_minima >= 0 AND faixa_preco_maxima >= faixa_preco_minima),
    CONSTRAINT ck_perfil_disponibilidade CHECK (disponibilidade IN ('IMEDIATA', 'EM_ATE_15_DIAS', 'EM_ATE_30_DIAS', 'INDISPONIVEL')),
    CONSTRAINT ck_perfil_modalidade CHECK (modalidade_trabalho IN ('REMOTO', 'PRESENCIAL', 'HIBRIDO')),
    CONSTRAINT ck_perfil_status CHECK (status IN ('ATIVO', 'PAUSADO'))
);

CREATE TABLE perfil_habilidade (
    perfil_id  BIGINT      NOT NULL,
    habilidade VARCHAR(40) NOT NULL,
    CONSTRAINT pk_perfil_habilidade PRIMARY KEY (perfil_id, habilidade),
    CONSTRAINT fk_perfil_habilidade_perfil FOREIGN KEY (perfil_id) REFERENCES perfil_profissional (id) ON DELETE CASCADE
);

CREATE TABLE categoria (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    slug      VARCHAR(40)  NOT NULL,
    nome      VARCHAR(60)  NOT NULL,
    sigla     VARCHAR(4)   NOT NULL,
    descricao VARCHAR(300) NOT NULL,
    CONSTRAINT pk_categoria PRIMARY KEY (id),
    CONSTRAINT uk_categoria_slug UNIQUE (slug)
);

CREATE TABLE servico (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    prestador_id  BIGINT        NOT NULL,
    categoria_id  BIGINT        NOT NULL,
    titulo        VARCHAR(120)  NOT NULL,
    descricao     VARCHAR(2000) NOT NULL,
    preco_base    DECIMAL(12,2) NOT NULL,
    prazo_dias    INT           NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    criado_em     DATETIME(6)   NOT NULL,
    atualizado_em DATETIME(6)   NOT NULL,
    version       BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_servico PRIMARY KEY (id),
    CONSTRAINT fk_servico_prestador FOREIGN KEY (prestador_id) REFERENCES usuario (id),
    CONSTRAINT fk_servico_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT ck_servico_preco CHECK (preco_base > 0),
    CONSTRAINT ck_servico_prazo CHECK (prazo_dias BETWEEN 1 AND 365),
    CONSTRAINT ck_servico_status CHECK (status IN ('ATIVO', 'PAUSADO'))
);
CREATE INDEX idx_servico_categoria_status ON servico (categoria_id, status);
CREATE INDEX idx_servico_prestador_status ON servico (prestador_id, status);

CREATE TABLE projeto (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    cliente_id       BIGINT        NOT NULL,
    categoria_id     BIGINT,
    titulo           VARCHAR(150)  NOT NULL,
    necessidade      VARCHAR(2000) NOT NULL,
    objetivo         VARCHAR(500),
    descricao        VARCHAR(4000),
    orcamento_minimo DECIMAL(12,2),
    orcamento_maximo DECIMAL(12,2),
    prazo_dias       INT,
    status           VARCHAR(20)   NOT NULL,
    publicado_em     DATETIME(6),
    criado_em        DATETIME(6)   NOT NULL,
    atualizado_em    DATETIME(6)   NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_projeto PRIMARY KEY (id),
    CONSTRAINT fk_projeto_cliente FOREIGN KEY (cliente_id) REFERENCES usuario (id),
    CONSTRAINT fk_projeto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT ck_projeto_orcamento CHECK (orcamento_minimo IS NULL OR orcamento_maximo IS NULL OR orcamento_maximo >= orcamento_minimo),
    CONSTRAINT ck_projeto_prazo CHECK (prazo_dias IS NULL OR prazo_dias BETWEEN 1 AND 365),
    CONSTRAINT ck_projeto_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'IN_NEGOTIATION', 'CONTRACTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX idx_projeto_status_categoria ON projeto (status, categoria_id);
CREATE INDEX idx_projeto_cliente_status ON projeto (cliente_id, status);

CREATE TABLE projeto_habilidade (
    projeto_id BIGINT      NOT NULL,
    habilidade VARCHAR(40) NOT NULL,
    CONSTRAINT pk_projeto_habilidade PRIMARY KEY (projeto_id, habilidade),
    CONSTRAINT fk_projeto_habilidade_projeto FOREIGN KEY (projeto_id) REFERENCES projeto (id) ON DELETE CASCADE
);

CREATE TABLE requisito (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    projeto_id BIGINT       NOT NULL,
    tipo       VARCHAR(20)  NOT NULL,
    descricao  VARCHAR(500) NOT NULL,
    ordem      INT          NOT NULL,
    CONSTRAINT pk_requisito PRIMARY KEY (id),
    CONSTRAINT fk_requisito_projeto FOREIGN KEY (projeto_id) REFERENCES projeto (id) ON DELETE CASCADE,
    CONSTRAINT ck_requisito_tipo CHECK (tipo IN ('REQUISITO', 'ENTREGAVEL', 'CRITERIO_ACEITE'))
);
CREATE INDEX idx_requisito_projeto ON requisito (projeto_id, ordem);

CREATE TABLE proposta (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    projeto_id    BIGINT        NOT NULL,
    prestador_id  BIGINT        NOT NULL,
    valor         DECIMAL(12,2) NOT NULL,
    prazo_dias    INT           NOT NULL,
    mensagem      VARCHAR(2000) NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    criada_em     DATETIME(6)   NOT NULL,
    respondida_em DATETIME(6),
    version       BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_proposta PRIMARY KEY (id),
    CONSTRAINT uk_proposta_projeto_prestador UNIQUE (projeto_id, prestador_id),
    CONSTRAINT fk_proposta_projeto FOREIGN KEY (projeto_id) REFERENCES projeto (id),
    CONSTRAINT fk_proposta_prestador FOREIGN KEY (prestador_id) REFERENCES usuario (id),
    CONSTRAINT ck_proposta_valor CHECK (valor > 0),
    CONSTRAINT ck_proposta_prazo CHECK (prazo_dias BETWEEN 1 AND 365),
    CONSTRAINT ck_proposta_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED'))
);
CREATE INDEX idx_proposta_prestador_status ON proposta (prestador_id, status);
CREATE INDEX idx_proposta_projeto_status ON proposta (projeto_id, status);

-- UNIQUE em projeto_id e proposta_id é a última barreira contra duas contratações
-- simultâneas para o mesmo projeto, mesmo que a verificação na aplicação falhe.
CREATE TABLE contratacao (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    projeto_id   BIGINT        NOT NULL,
    proposta_id  BIGINT        NOT NULL,
    valor        DECIMAL(12,2) NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    criada_em    DATETIME(6)   NOT NULL,
    iniciada_em  DATETIME(6),
    concluida_em DATETIME(6),
    version      BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_contratacao PRIMARY KEY (id),
    CONSTRAINT uk_contratacao_projeto UNIQUE (projeto_id),
    CONSTRAINT uk_contratacao_proposta UNIQUE (proposta_id),
    CONSTRAINT fk_contratacao_projeto FOREIGN KEY (projeto_id) REFERENCES projeto (id),
    CONSTRAINT fk_contratacao_proposta FOREIGN KEY (proposta_id) REFERENCES proposta (id),
    CONSTRAINT ck_contratacao_valor CHECK (valor > 0),
    CONSTRAINT ck_contratacao_status CHECK (status IN ('AWAITING_PAYMENT', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX idx_contratacao_status ON contratacao (status);

CREATE TABLE entrega (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    contratacao_id   BIGINT        NOT NULL,
    titulo           VARCHAR(500)  NOT NULL,
    ordem            INT           NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    conteudo_envio   VARCHAR(2000),
    feedback_revisao VARCHAR(1000),
    enviada_em       DATETIME(6),
    avaliada_em      DATETIME(6),
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_entrega PRIMARY KEY (id),
    CONSTRAINT fk_entrega_contratacao FOREIGN KEY (contratacao_id) REFERENCES contratacao (id),
    CONSTRAINT ck_entrega_status CHECK (status IN ('PENDING', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REVISION_REQUIRED'))
);
CREATE INDEX idx_entrega_contratacao_status ON entrega (contratacao_id, status);

CREATE TABLE pagamento (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    contratacao_id BIGINT        NOT NULL,
    txid           VARCHAR(35)   NOT NULL,
    valor          DECIMAL(12,2) NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    modo           VARCHAR(10)   NOT NULL,
    payload_pix    VARCHAR(512)  NOT NULL,
    criado_em      DATETIME(6)   NOT NULL,
    expira_em      DATETIME(6)   NOT NULL,
    pago_em        DATETIME(6),
    version        BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_pagamento PRIMARY KEY (id),
    CONSTRAINT uk_pagamento_txid UNIQUE (txid),
    CONSTRAINT fk_pagamento_contratacao FOREIGN KEY (contratacao_id) REFERENCES contratacao (id),
    CONSTRAINT ck_pagamento_valor CHECK (valor > 0),
    CONSTRAINT ck_pagamento_status CHECK (status IN ('CREATED', 'PENDING', 'PAID', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_pagamento_modo CHECK (modo IN ('MOCK', 'REAL'))
);
CREATE INDEX idx_pagamento_contratacao ON pagamento (contratacao_id, status);
CREATE INDEX idx_pagamento_status_expira ON pagamento (status, expira_em);

CREATE TABLE notificacao (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT       NOT NULL,
    tipo       VARCHAR(40)  NOT NULL,
    mensagem   VARCHAR(300) NOT NULL,
    link       VARCHAR(200),
    lida       BOOLEAN      NOT NULL DEFAULT FALSE,
    criada_em  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_notificacao PRIMARY KEY (id),
    CONSTRAINT fk_notificacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
);
CREATE INDEX idx_notificacao_usuario_lida ON notificacao (usuario_id, lida, criada_em);

-- Sem FK para usuario de propósito: o registro precisa sobreviver à anonimização da conta
-- e não deve ser apagado em cascata. usuario_id aqui é um identificador pseudonimizado.
CREATE TABLE registro_auditoria (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    usuario_id   BIGINT,
    evento       VARCHAR(40) NOT NULL,
    recurso_tipo VARCHAR(30),
    recurso_id   BIGINT,
    ip           VARCHAR(45),
    criado_em    DATETIME(6) NOT NULL,
    CONSTRAINT pk_registro_auditoria PRIMARY KEY (id)
);
CREATE INDEX idx_auditoria_usuario_data ON registro_auditoria (usuario_id, criado_em);
CREATE INDEX idx_auditoria_data ON registro_auditoria (criado_em);
