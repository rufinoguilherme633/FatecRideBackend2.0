-- Test Schema Initialization for H2 Database

CREATE TABLE IF NOT EXISTS status_fila_motoristas (
    id_status_fila BIGINT PRIMARY KEY AUTO_INCREMENT,
    status_nome VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS status_solicitacao (
    id_status_solicitacao BIGINT PRIMARY KEY AUTO_INCREMENT,
    status_nome VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS status_pipeline_solicitacao (
    id_status_pipeline BIGINT PRIMARY KEY AUTO_INCREMENT,
    status_nome VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS status_carona (
    id_status_carona BIGINT PRIMARY KEY AUTO_INCREMENT,
    status_nome VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS curso (
    id_curso BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS genero (
    id_genero BIGINT PRIMARY KEY AUTO_INCREMENT,
    genero VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS tipo_usuario (
    id_tipo_usuario BIGINT PRIMARY KEY AUTO_INCREMENT,
    tipo VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS estados (
    id_estado BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    uf VARCHAR(10),
    ibge VARCHAR(20),
    pais VARCHAR(100),
    ddd VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS cidade (
    id_cidade BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    id_estado BIGINT,
    ibge VARCHAR(20),
    FOREIGN KEY (id_estado) REFERENCES estados(id_estado)
);

CREATE TABLE IF NOT EXISTS origens (
    id_origem BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_cidade BIGINT,
    logradouro VARCHAR(255),
    numero VARCHAR(50),
    bairro VARCHAR(100),
    cep VARCHAR(20),
    latitude DOUBLE,
    longitude DOUBLE,
    FOREIGN KEY (id_cidade) REFERENCES cidade(id_cidade)
);

CREATE TABLE IF NOT EXISTS destinos (
    id_destino BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_cidade BIGINT,
    logradouro VARCHAR(255),
    numero VARCHAR(50),
    bairro VARCHAR(100),
    cep VARCHAR(20),
    latitude DOUBLE,
    longitude DOUBLE,
    FOREIGN KEY (id_cidade) REFERENCES cidade(id_cidade)
);

CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    sobrenome VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255),
    telefone VARCHAR(20),
    foto VARCHAR(255),
    id_tipo_usuario BIGINT,
    id_genero BIGINT,
    id_curso BIGINT,
    FOREIGN KEY (id_tipo_usuario) REFERENCES tipo_usuario(id_tipo_usuario),
    FOREIGN KEY (id_genero) REFERENCES genero(id_genero),
    FOREIGN KEY (id_curso) REFERENCES curso(id_curso)
);

CREATE TABLE IF NOT EXISTS veiculos (
    id_veiculo BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_usuario BIGINT,
    modelo VARCHAR(100),
    marca VARCHAR(100),
    placa VARCHAR(20),
    cor VARCHAR(50),
    ano INT,
    vagas_disponiveis INT DEFAULT 4,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE TABLE IF NOT EXISTS caronas (
    id_carona BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_motorista BIGINT,
    id_origem BIGINT,
    id_destino BIGINT,
    data_hora TIMESTAMP,
    vagas_disponiveis INT DEFAULT 4,
    id_status_carona BIGINT,
    id_veiculo BIGINT,
    data_ride DATE,
    FOREIGN KEY (id_motorista) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_origem) REFERENCES origens(id_origem),
    FOREIGN KEY (id_destino) REFERENCES destinos(id_destino),
    FOREIGN KEY (id_status_carona) REFERENCES status_carona(id_status_carona),
    FOREIGN KEY (id_veiculo) REFERENCES veiculos(id_veiculo)
);

CREATE TABLE IF NOT EXISTS solicitacoes (
    id_solicitacao BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_carona BIGINT,
    id_passageiro BIGINT,
    id_origem BIGINT,
    id_destino BIGINT,
    data_solicitacao TIMESTAMP,
    id_status_solicitacao BIGINT DEFAULT 1,
    tentativa_atual INT DEFAULT 0,
    id_status_pipeline BIGINT,
    FOREIGN KEY (id_carona) REFERENCES caronas(id_carona),
    FOREIGN KEY (id_passageiro) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_origem) REFERENCES origens(id_origem),
    FOREIGN KEY (id_destino) REFERENCES destinos(id_destino),
    FOREIGN KEY (id_status_solicitacao) REFERENCES status_solicitacao(id_status_solicitacao),
    FOREIGN KEY (id_status_pipeline) REFERENCES status_pipeline_solicitacao(id_status_pipeline)
);

CREATE TABLE IF NOT EXISTS solicitacao_fila_motoristas (
    id_fila BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_solicitacao BIGINT,
    id_motorista BIGINT,
    id_carona BIGINT,
    ordem_fila INT,
    data_envio TIMESTAMP,
    data_resposta TIMESTAMP,
    id_status_fila BIGINT,
    distancia_origem_km DOUBLE,
    FOREIGN KEY (id_solicitacao) REFERENCES solicitacoes(id_solicitacao),
    FOREIGN KEY (id_motorista) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_carona) REFERENCES caronas(id_carona),
    FOREIGN KEY (id_status_fila) REFERENCES status_fila_motoristas(id_status_fila)
);
