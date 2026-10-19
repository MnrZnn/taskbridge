-- Contas criadas por login social (Google/GitHub) começam sem senha própria.
ALTER TABLE usuario ADD COLUMN possui_senha BOOLEAN NOT NULL DEFAULT TRUE;

-- Vínculo entre uma conta e uma identidade num provedor externo. O "sujeito" é o id estável
-- que o provedor dá ao usuário (não o e-mail, que pode mudar).
CREATE TABLE identidade_externa (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT       NOT NULL,
    provedor   VARCHAR(20)  NOT NULL,
    sujeito    VARCHAR(100) NOT NULL,
    criada_em  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_identidade_provedor_sujeito UNIQUE (provedor, sujeito),
    CONSTRAINT uk_identidade_usuario_provedor UNIQUE (usuario_id, provedor),
    CONSTRAINT fk_identidade_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT ck_identidade_provedor CHECK (provedor IN ('GOOGLE', 'GITHUB'))
);

-- Links públicos do perfil (LinkedIn, GitHub, portfólio...). Limite de quantidade é regra de domínio.
CREATE TABLE usuario_link (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT       NOT NULL,
    url        VARCHAR(300) NOT NULL,
    ordem      INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_usuario_link_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
);
CREATE INDEX idx_usuario_link_usuario ON usuario_link (usuario_id, ordem);
