-- Script SQL para Etapa 2: Adicionar novos status ao projeto
-- Executar manualmente no banco de dados antes de usar a aplicação

-- Inserir novos status em status_solicitacao
INSERT IGNORE INTO status_solicitacao (id_status_solicitacao, status_nome) VALUES
(6, 'aguardando_resposta'),
(7, 'falha_final');

-- Criar e popular tabela status_pipeline_solicitacao
INSERT IGNORE INTO status_pipeline_solicitacao (id_status_pipeline, status_nome) VALUES
(1, 'aguardando'),
(2, 'aceita'),
(3, 'falha_final');

