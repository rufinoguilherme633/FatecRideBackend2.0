-- Test Data Initialization Script for H2 Database

-- Insert status for fila (queue)
INSERT INTO status_fila_motoristas (id_status_fila, status_nome) VALUES
(1, 'pendente'),
(2, 'enviada'),
(3, 'aceita'),
(4, 'recusada'),
(5, 'timeout');

-- Insert status for solicitação (request)
INSERT INTO status_solicitacao (id_status_solicitacao, status_nome) VALUES
(1, 'pendente'),
(2, 'aceita'),
(3, 'recusada'),
(4, 'cancelada'),
(5, 'concluida'),
(6, 'aguardando_resposta'),
(7, 'falha_final');

-- Insert status for pipeline
INSERT INTO status_pipeline_solicitacao (id_status_pipeline, status_nome) VALUES
(1, 'aguardando'),
(2, 'aceita'),
(3, 'falha_final');

