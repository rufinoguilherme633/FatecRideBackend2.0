-- Test Schema Initialization for H2 Database

-- Create status_fila_motoristas table
CREATE TABLE IF NOT EXISTS status_fila_motoristas (
    id_status_fila BIGINT PRIMARY KEY AUTO_INCREMENT,
    status_nome VARCHAR(50) NOT NULL UNIQUE
);

-- Create status_solicitacao table
CREATE TABLE IF NOT EXISTS status_solicitacao (
    id_status_solicitacao BIGINT PRIMARY KEY AUTO_INCREMENT,
    status_nome VARCHAR(50) NOT NULL UNIQUE
);

-- Create status_pipeline_solicitacao table
CREATE TABLE IF NOT EXISTS status_pipeline_solicitacao (
    id_status_pipeline BIGINT PRIMARY KEY AUTO_INCREMENT,
    status_nome VARCHAR(50) NOT NULL UNIQUE
);

-- Create other essential tables for testing
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    foto LONGBLOB
);

CREATE TABLE IF NOT EXISTS cursos (
    id_curso BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome_curso VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS generos (
    id_genero BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome_genero VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS veiculos (
    id_veiculo BIGINT PRIMARY KEY AUTO_INCREMENT,
    modelo VARCHAR(100),
    marca VARCHAR(100),
    placa VARCHAR(20),
    cor VARCHAR(50),
    id_usuario BIGINT,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE TABLE IF NOT EXISTS caronas (
    id_carona BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_motorista BIGINT,
    id_veiculo BIGINT,
    vagas_disponiveis INT DEFAULT 4,
    FOREIGN KEY (id_motorista) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_veiculo) REFERENCES veiculos(id_veiculo)
);

CREATE TABLE IF NOT EXISTS origem (
    id_origem BIGINT PRIMARY KEY AUTO_INCREMENT,
    latitude DOUBLE,
    longitude DOUBLE,
    logradouro VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS destino (
    id_destino BIGINT PRIMARY KEY AUTO_INCREMENT,
    latitude DOUBLE,
    longitude DOUBLE,
    logradouro VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS solicitacoes (
    id_solicitacao BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_passageiro BIGINT,
    id_carona BIGINT,
    id_origem BIGINT,
    id_destino BIGINT,
    id_status_solicitacao BIGINT DEFAULT 1,
    tentativa_atual INT DEFAULT 0,
    id_status_pipeline BIGINT,
    FOREIGN KEY (id_passageiro) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_carona) REFERENCES caronas(id_carona),
    FOREIGN KEY (id_origem) REFERENCES origem(id_origem),
    FOREIGN KEY (id_destino) REFERENCES destino(id_destino),
    FOREIGN KEY (id_status_solicitacao) REFERENCES status_solicitacao(id_status_solicitacao),
    FOREIGN KEY (id_status_pipeline) REFERENCES status_pipeline_solicitacao(id_status_pipeline)
);

CREATE TABLE IF NOT EXISTS solicitacao_fila_motoristas (
    id_fila BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_solicitacao BIGINT,
    id_motorista BIGINT,
    id_carona BIGINT,
    ordem_fila INT,
    data_envio DATETIME,
    data_resposta DATETIME,
    id_status_fila BIGINT,
    distancia_origem_km DOUBLE,
    FOREIGN KEY (id_solicitacao) REFERENCES solicitacoes(id_solicitacao),
    FOREIGN KEY (id_motorista) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_carona) REFERENCES caronas(id_carona),
    FOREIGN KEY (id_status_fila) REFERENCES status_fila_motoristas(id_status_fila)
);

