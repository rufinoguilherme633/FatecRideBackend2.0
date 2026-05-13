# 🚀 Guia de Validação E2E com Insomnia

## 📍 Pré-requisitos

1. **Insomnia instalado** (https://insomnia.rest/)
2. **API rodando** em `http://localhost:8080`
3. **Dois tokens JWT válidos** (passageiro e 2 motoristas)
4. **SSE suportado** no Insomnia (versão 2022+)

---

## 🔧 Passo 1: Importar Coleção

1. Abrar **Insomnia**
2. Clicar em **Dashboard** → **Import**
3. Selecionar `Insomnia_Fluxo_Automatico.json`
4. Confirmar import

✅ Você verá uma coleção com 4 pastas:
- ✅ CENÁRIO A - Motorista ACEITA
- 🔄 CENÁRIO B - Motorista RECUSA → Próximo ACEITA
- ❌ CENÁRIO C - 3 Tentativas FALHAM
- 🛡️ TESTES NEGATIVOS

---

## 📋 Passo 2: Configurar Variáveis de Ambiente

1. Clicar em **Insomnia** → **Preferences** → **Data**
2. Abrir aba **Environments**
3. Selecionar **Fluxo Automático - Local**
4. Preencher as variáveis:

```json
{
  "baseUrl": "http://localhost:8080",
  "solicitacaoId": "1",
  "filaId": "",
  "fila2Id": "",
  "tokenPassageiro": "seu_token_aqui",
  "tokenMotorista1": "token_motorista_1",
  "tokenMotorista2": "token_motorista_2",
  "motoristaId1": "2",
  "motoristaId2": "3",
  "passageiroId": "1"
}
```

### Como gerar tokens?

Se sua API tem endpoint de login:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"passageiro@example.com\",\"password\":\"senha\"}"
```

Copiar o `token` da resposta e colar em `tokenPassageiro`.

---

## ✅ Passo 3: Executar CENÁRIO A (Happy Path)

### Estado Inicial
- Solicitação 1 : status PENDENTE, pipeline AGUARDANDO
- **Resultado esperado:** Motorista aceita direto

### Executar em ordem:

1. **1️⃣ Passageiro inicia fluxo**
   - Request: `POST /solicitacao/automatico/iniciar`
   - Copiar `filaId` da resposta para variável

2. **2️⃣ [SSE] Motorista conecta** (em aba separada)
   - Request: `GET /notificacoes/stream?token=...`
   - Deixar aberta (streaming ativo)
   - ✅ Você verá evento: `nova_solicitacao` com `filaId`, `solicitacaoId`, dados corrida

3. **3️⃣ Motorista ACEITA**
   - Request: `POST /solicitacao/automatico/{{ filaId }}/aceitar/1`
   - Copiar `filaId` da resposta da etapa 1
   - ✅ Status 200, message: "Solicitação aceita com sucesso"

4. **4️⃣ [SSE] Passageiro recebe notificação** (aba SSE do passageiro)
   - ✅ Você verá evento: `solicitacao_aceita` com motorista confirmado

5. **5️⃣ [SQL] Validar BD**
   - Abrir ferramenta de BD (DBeaver, MySQL Workbench, etc.)
   - Executar queries para confirmar estados:

```sql
-- Deve estar: status=ACEITA, pipeline=ACEITA, tentativa_atual=1
SELECT id_solicitacao, status_solicitacao, id_status_pipeline, tentativa_atual
FROM solicitacoes WHERE id_solicitacao = 1;

-- Deve estar: 1 fila ACEITA com data_resposta preenchida
SELECT id_fila, id_motorista, id_status_fila, data_envio, data_resposta
FROM solicitacao_fila_motoristas WHERE id_solicitacao = 1;
```

✅ **Cenário A PASSOU** se BD mostrar:
| id_solicitacao | status_solicitacao | id_status_pipeline | tentativa_atual |
|---|---|---|---|
| 1 | 2 (aceita) | 2 (aceita) | 1 |

---

## 🔄 Passo 4: Executar CENÁRIO B (Recusa → Próximo Aceita)

### Estado Inicial
- Solicitação 2: nova, status PENDENTE
- 2+ motoristas na fila
- **Resultado esperado:** Motorista 1 recusa, Motorista 2 aceita

### Executar em ordem:

1. **1️⃣ Passageiro inicia fluxo (solicitação 2)**
   - Mudar `solicitacaoId` para `2`
   - Copiar `filaId` para variável

2. **2️⃣ Motorista 1 RECUSA**
   - Request: `POST /solicitacao/automatico/{{ filaId }}/recusar/2`
   - ✅ Status 200, fila1 marcada RECUSADA
   - ✅ SSE: Motorista 2 recebe `nova_solicitacao`

3. **3️⃣ Motorista 2 ACEITA**
   - Mudar token para `tokenMotorista2`
   - Copiar novo `filaId` da SSE da etapa 2
   - Request: `POST /solicitacao/automatico/{{ fila2Id }}/aceitar/2`
   - ✅ Status 200, fila2 marcada ACEITA

4. **4️⃣ [SQL] Validar BD**

```sql
-- Deve estar: tentativa_atual = 2 (incrementou após recusa)
SELECT id_solicitacao, tentativa_atual FROM solicitacoes WHERE id_solicitacao = 2;

-- Deve estar: fila1 RECUSADA, fila2 ACEITA
SELECT id_fila, id_motorista, id_status_fila, data_resposta
FROM solicitacao_fila_motoristas WHERE id_solicitacao = 2 ORDER BY id_fila;
```

✅ **Cenário B PASSOU** se:
- Fila 1: status RECUSADA (4) com data_resposta
- Fila 2: status ACEITA (3) com data_resposta
- tentativa_atual = 2

---

## ❌ Passo 5: Executar CENÁRIO C (3 Falhas → FALHA_FINAL)

### Estado Inicial
- Solicitação 3: nova, status PENDENTE
- 3+ motoristas na fila
- **Resultado esperado:** 3 tentativas falham (recusa/timeout), sistema para

### ⚠️ ATENÇÃO: Este cenário leva ~60 segundos (timeout)

### Executar em ordem:

1. **1️⃣ Passageiro inicia fluxo (solicitação 3)**
   - Mudar `solicitacaoId` para `3`

2. **2️⃣ Tentativa 1 - Motorista RECUSA**
   - Request: `POST /solicitacao/automatico/{{ filaId }}/recusar/3`
   - ✅ BD: tentativa_atual = 2

3. **3️⃣ Tentativa 2 - Motorista TIMEOUT (aguardar 60s)**
   - SSE conecta: `GET /notificacoes/stream?token=...`
   - Aguardar ~60 segundos (timeout automático)
   - ✅ BD: tentativa_atual = 3

4. **4️⃣ Tentativa 3 - Motorista RECUSA**
   - Request: `POST /solicitacao/automatico/{{ filaId }}/recusar/3`
   - ✅ Status 200 ou erro (limite atingido)

5. **5️⃣ [SQL] Validar BD**

```sql
-- Deve estar: status=RECUSADA, pipeline=FALHA_FINAL, tentativa_atual=3
SELECT id_solicitacao, status_solicitacao, id_status_pipeline, tentativa_atual
FROM solicitacoes WHERE id_solicitacao = 3;

-- Todas as filas devem ter data_resposta preenchida (não ficar NULL)
SELECT id_fila, id_motorista, id_status_fila, data_resposta
FROM solicitacao_fila_motoristas WHERE id_solicitacao = 3 ORDER BY id_fila;
```

✅ **Cenário C PASSOU** se:
- Solicitação: status RECUSADA (3), pipeline FALHA_FINAL (3), tentativa_atual = 3
- Todas as filas têm data_resposta preenchida
- Sistema não tenta 4ª motorista

---

## 🛡️ Passo 6: Testes Negativos (Segurança)

Confirmam que o sistema **rejeita** operações inválidas.

### ❌ Teste 1: Motorista ERRADO tenta aceitar
- Usa token do Motorista 2 mas tenta aceitar fila do Motorista 1
- ✅ Esperado: **Status 400/403** (erro de autorização)
- 🐛 Se aceitar = BUG CRÍTICO de segurança

### ❌ Teste 2: IDs inconsistentes (filaId ≠ solicitacaoId)
- filaId: 999 | solicitacaoId: 1
- ✅ Esperado: **Status 404 ou 400** (não encontrado)
- 🐛 Se processar = BUG CRÍTICO

### ❌ Teste 3: Recusa SEM autenticação
- Remove header Authorization
- ✅ Esperado: **Status 401** (não autenticado)
- 🐛 Se processar = BUG CRÍTICO

### ❌ Teste 4: Aceita fila já ACEITA
- Tenta aceitar novamente após Cenário A
- ✅ Esperado: **Status 409 ou 400** (estado inválido)
- 🐛 Se aceitar = BUG CRÍTICO

---

## 📊 Checklist Final

Após executar A, B, C e testes negativos:

- [ ] **Cenário A**: Motorista aceita direto ✅
- [ ] **Cenário B**: Recusa + próximo aceita ✅
- [ ] **Cenário C**: 3 falhas = FALHA_FINAL ✅
- [ ] **tentativa_atual**: 0 → 1 → 2 → 3 (depois para) ✅
- [ ] **data_resposta**: Sempre preenchida ✅
- [ ] **SSE motorista**: Recebe nova_solicitacao ✅
- [ ] **SSE passageiro**: Recebe solicitacao_aceita ou falha_final ✅
- [ ] **Segurança**: Rejeita motorista errado ✅
- [ ] **Segurança**: Rejeita IDs inconsistentes ✅
- [ ] **Segurança**: Rejeita sem autenticação ✅

---

## 🚨 Se algo falhar

1. **Verificar logs** da API:
```powershell
# Terminal onde rodou spring-boot:run
# Procurar por erros de transação, validação, timeout
```

2. **Resetar BD** para limpar dados de testes:
```sql
DELETE FROM solicitacao_fila_motoristas WHERE id_solicitacao IN (1,2,3);
DELETE FROM solicitacoes WHERE id_solicitacao IN (1,2,3);
```

3. **Verificar tokens** estão válidos:
```bash
curl http://localhost:8080/auth/validate -H "Authorization: Bearer SEU_TOKEN"
```

4. **SSE não funciona?**
   - Confirmar browser/Insomnia suporta streaming
   - Verificar CORS em `SecurityConfig.java`

---

## 📝 Notas Importantes

- **Cenário C leva ~60s** (timeout configurável em `application.properties`)
- **SSE é unidirecional** (cliente recebe, não envia)
- **IDs hardcoded** (solicitacaoId 1,2,3 esperados no BD de teste)
- **Tokens precisam ser válidos** e corresponder aos usuários

---

**Pronto para validação! 🎯**

Se tudo passar, o fluxo está pronto para produção.

