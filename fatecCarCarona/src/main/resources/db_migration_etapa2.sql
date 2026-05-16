-- Etapa 2: Script de migração manual para inserir novos status
-- Executar no banco de dados de produção após deploy

-- Inserir novos status em status_fila_motoristas
INSERT IGNORE INTO status_fila_motoristas (id_status_fila, status_nome) VALUES
(1, 'pendente'),
(2, 'enviada'),
(3, 'aceita'),
(4, 'recusada'),
(5, 'timeout');

-- Inserir novos status em status_solicitacao
INSERT IGNORE INTO status_solicitacao (id_status_solicitacao, status_nome) VALUES
(6, 'aguardando_resposta'),
(7, 'falha_final');

-- Populando status_pipeline_solicitacao
INSERT IGNORE INTO status_pipeline_solicitacao (id_status_pipeline, status_nome) VALUES
(1, 'aguardando'),
(2, 'aceita'),
(3, 'falha_final');

-- Verificação: Selecionar todos os status para confirmar
SELECT 'Status Fila Motoristas:' as tabela, id_status_fila as id, status_nome as nome FROM status_fila_motoristas
UNION ALL
SELECT 'Status Solicitacao:', id_status_solicitacao, status_nome FROM status_solicitacao
UNION ALL
SELECT 'Status Pipeline:', id_status_pipeline, status_nome FROM status_pipeline_solicitacao;

