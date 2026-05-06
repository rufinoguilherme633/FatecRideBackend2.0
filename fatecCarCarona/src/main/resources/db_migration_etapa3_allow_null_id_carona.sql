-- Etapa 3: Tornar a coluna id_carona nullable para suportar solicitações sem carona vinculada
-- Execute este script no banco de dados (MySQL) do ambiente onde o aplicativo está rodando.
-- Faça backup antes de aplicar em produção.

ALTER TABLE solicitacoes
  MODIFY COLUMN id_carona BIGINT NULL;

-- Verificação opcional: mostrar definição da coluna (MySQL)
-- SHOW COLUMNS FROM solicitacoes LIKE 'id_carona';

