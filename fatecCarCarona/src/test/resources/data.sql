-- Test Data Initialization Script for H2 Database

MERGE INTO status_fila_motoristas (id_status_fila, status_nome) VALUES
    (1, 'pendente'),
    (2, 'enviada'),
    (3, 'aceita'),
    (4, 'recusada'),
    (5, 'timeout');

MERGE INTO status_solicitacao (id_status_solicitacao, status_nome) VALUES
    (1, 'pendente'),
    (2, 'aceita'),
    (3, 'recusada'),
    (4, 'cancelada'),
    (5, 'concluida'),
    (6, 'aguardando_resposta'),
    (7, 'falha_final');

MERGE INTO status_pipeline_solicitacao (id_status_pipeline, status_nome) VALUES
    (1, 'aguardando'),
    (2, 'aceita'),
    (3, 'falha_final');

MERGE INTO status_carona (id_status_carona, status_nome) VALUES
    (1, 'ativa'),
    (2, 'concluida');

MERGE INTO curso (id_curso, nome) VALUES
    (1, 'Engenharia da Computacao'),
    (2, 'Sistemas de Informacao');

MERGE INTO genero (id_genero, genero) VALUES
    (1, 'Masculino'),
    (2, 'Feminino');

MERGE INTO tipo_usuario (id_tipo_usuario, tipo) VALUES
    (1, 'passageiro'),
    (2, 'motorista');

MERGE INTO estados (id_estado, nome, uf, ibge, pais, ddd) VALUES
    (1, 'Sao Paulo', 'SP', '35', 'Brasil', '11');

MERGE INTO cidade (id_cidade, nome, id_estado, ibge) VALUES
    (1, 'Sao Paulo', 1, '3550308');

MERGE INTO origens (id_origem, id_cidade, logradouro, numero, bairro, cep, latitude, longitude) VALUES
    (1, 1, 'Avenida Paulista', '1000', 'Bela Vista', '01310-100', -23.5505, -46.6333);

MERGE INTO destinos (id_destino, id_cidade, logradouro, numero, bairro, cep, latitude, longitude) VALUES
    (1, 1, 'Rua Augusta', '1500', 'Consolacao', '01304-001', -23.5710, -46.6560);

MERGE INTO usuarios (id_usuario, nome, sobrenome, email, senha, telefone, foto, id_tipo_usuario, id_genero, id_curso) VALUES
    (1, 'Maria', 'Silva', 'maria@example.com', 'senha-maria', '11999999999', NULL, 1, 2, 1);

MERGE INTO usuarios (id_usuario, nome, sobrenome, email, senha, telefone, foto, id_tipo_usuario, id_genero, id_curso) VALUES
    (2, 'Joao', 'Santos', 'joao@example.com', 'senha-joao', '11988888888', NULL, 2, 1, 1);

MERGE INTO veiculos (id_veiculo, id_usuario, modelo, marca, placa, cor, ano, vagas_disponiveis) VALUES
    (1, 2, 'Civic', 'Honda', 'ABC-1234', 'Preto', 2020, 3);

MERGE INTO caronas (id_carona, id_motorista, id_origem, id_destino, data_hora, vagas_disponiveis, id_status_carona, id_veiculo) VALUES
    (1, 2, 1, 1, TIMESTAMP '2026-04-28 10:00:00', 3, 1, 1);

MERGE INTO solicitacoes (id_solicitacao, id_carona, id_passageiro, id_origem, id_destino, data_solicitacao, id_status_solicitacao, tentativa_atual, id_status_pipeline, version) VALUES
    (1, 1, 1, 1, 1, TIMESTAMP '2026-04-28 09:55:00', 1, 0, 1, 0);
