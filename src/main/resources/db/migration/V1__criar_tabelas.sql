CREATE TABLE parceiro (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    limite_credito NUMERIC(15,2) NOT NULL DEFAULT 0,
    credito_disponivel NUMERIC(15,2) NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE pedido (
    id BIGSERIAL PRIMARY KEY,
    parceiro_id BIGINT NOT NULL REFERENCES parceiro(id),
    valor_total NUMERIC(15,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE item_pedido (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
    produto VARCHAR(255) NOT NULL,
    quantidade INT NOT NULL,
    preco_unitario NUMERIC(15,2) NOT NULL
);
