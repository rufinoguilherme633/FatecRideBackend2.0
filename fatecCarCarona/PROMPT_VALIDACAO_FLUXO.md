# 📋 Prompt para Validação Completa do Fluxo Automático de Carona

## 🎯 Objetivo
Realizar uma auditoria técnica completa do fluxo automático de carona, identificando bugs, erros lógicos, código incorreto e inconsistências com a regra de negócio definida.

---

## 📐 REGRA DE NEGÓCIO

### Flow Esperado (Correto)

```
1. PASSAGEIRO CRIA SOLICITAÇÃO
   - POST /solicitacao
   - Status inicial: PENDENTE
   - Database: solicitacoes (status_solicitacao = 1)

2. PASSAGEIRO INICIA FLUXO AUTOMÁTICO
   - POST /solicitacao/automatico/iniciar
   - Sistema busca motoristas próximos
   - Cria fila de motoristas (solicitacao_fila_motoristas)
   - Envia primeira notificação via SSE para motorista 1
   - Status da fila: ENVIADA (2)
   - Pipeline status: AGUARDANDO

3. MOTORISTA RECEBE NOTIFICAÇÃO
   - Via SSE: nova_solicitacao event
   - Inclui: solicitacaoId, filaId, dados da corrida
   - Motorista tem 60 segundos para responder (configurável)

4. MOTORISTA RESPONDE (Opção A: ACEITA)
   ┌─ POST /solicitacao/automatico/{filaId}/aceitar/{solicitacaoId}
   ├─ Status da fila: ACEITA (3)
   ├─ data_resposta: NOW()
   ├─ Solicitação status: ACEITA (2)
   ├─ Pipeline status: ACEITA
   ├─ Rejeita automaticamente outros motoristas na fila
   └─ Notifica passageiro via SSE: solicitacao_aceita ✅

4. MOTORISTA RESPONDE (Opção B: RECUSA)
   ┌─ POST /solicitacao/automatico/{filaId}/recusar/{solicitacaoId}
   ├─ Status da fila: RECUSADA (4)
   ├─ data_resposta: NOW()
   ├─ Tenta próximo motorista na fila
   ├─ Se houver próximo: envia notificação, volta ao passo 3
   └─ Se não houver: marca solicitação como RECUSADA ❌

5. TIMEOUT (Motorista não responde em 60s)
   ┌─ handleTimeoutMotorista() é acionado automaticamente
   ├─ Status da fila: TIMEOUT (5)
   ├─ data_resposta: NOW()
   ├─ Tenta próximo motorista na fila
   ├─ Se houver próximo: envia notificação, volta ao passo 3
   └─ Se não houver: marca solicitação como RECUSADA ❌

6. LIMITE DE TENTATIVAS ATINGIDO
   ┌─ Após 3 tentativas falhadas (recusa + timeout)
   ├─ Solicitação status: RECUSADA (3)
   ├─ Pipeline status: FALHA_FINAL
   ├─ data_resposta definida para TODOS os registros na fila
   └─ Notifica passageiro: nenhum_motorista ❌

7. PASSAGEIRO VERIFICA STATUS
   ├─ GET /solicitacao/pending
   ├─ Se aceita: mostra motorista confirmado ✅
   ├─ Se recusada: aviso de nenhum motorista ❌
   └─ Pode cancelar se ainda estiver pendente
```

### Estados de Banco de Dados

**Tabela: solicitacoes**
```
┌──────┬────────┬──────────┬──────────────┬───────────────┐
│ id   │ status │ pipeline │ tentativa_id │ data_hora     │
├──────┼────────┼──────────┼──────────────┼───────────────┤
│ 1    │ ACEITA │ ACEITA   │ 1            │ 2026-05-06... │
└──────┴────────┴──────────┴──────────────┴───────────────┘
```

**Tabela: solicitacao_fila_motoristas**
```
┌────┬──────────┬───────────┬────────────┬───────────────┬────────────────┐
│ id │ motorista│ carona    │ status     │ data_envio    │ data_resposta  │
├────┼──────────┼───────────┼────────────┼───────────────┼────────────────┤
│ 1  │ 2 (Maria)│ 1         │ ACEITA (3) │ 14:00:05      │ 14:00:12       │
│ 2  │ 3 (João) │ 2         │ RECUSADA   │ 14:02:00      │ 14:02:30       │
│ 3  │ 4 (Pedro)│ 3         │ TIMEOUT    │ 14:03:00      │ 14:04:00       │
└────┴──────────┴───────────┴────────────┴───────────────┴────────────────┘
```

---

## 🔍 CHECKLIST DE VALIDAÇÃO

### A. ENDPOINTS

- [ ] `POST /solicitacao/automatico/iniciar` 
  - Existe? ✅
  - Cria fila? ✅
  - Inicia timer de timeout? ✅
  - Status correto no BD? ✅

- [ ] `POST /solicitacao/automatico/{filaId}/aceitar/{solicitacaoId}` (NOVO)
  - Existe? ✅
  - Valida autenticação? ✅
  - Usa filaId e solicitacaoId? ✅
  - Atualiza status corretamente? ✅
  - Rejeita outros motoristas? ✅
  - Notifica passageiro? ✅
  - Retorna JSON esperado? ✅

- [ ] `POST /solicitacao/automatico/{filaId}/recusar/{solicitacaoId}` (NOVO)
  - Existe? ✅
  - Marca como RECUSADA? ✅
  - Tenta próximo motorista? ✅
  - Notifica corretamente? ✅
  - Manipula erro se sem próximo? ✅

### B. LÓGICA DE NEGÓCIO

- [ ] **Aceitação (Happy Path)**
  - Fila marcada como ACEITA
  - Solicitação marcada como ACEITA
  - data_resposta preenchida
  - Outros motoristas rejeitados
  - Passageiro notificado
  - Status pipeline: ACEITA

- [ ] **Recusa**
  - Fila marcada como RECUSADA
  - data_resposta preenchida
  - Próximo motorista da fila recebe notificação
  - tentativa_atual incrementa
  - Se sem próximo: solicitação marcada como RECUSADA

- [ ] **Timeout**
  - Fila marcada como TIMEOUT
  - Timer de 60s funciona corretamente
  - Próximo motorista acionado
  - tentativa_atual incrementa
  - Se sem próximo: solicitação marcada como RECUSADA

- [ ] **Limite de Tentativas**
  - Após 3 tentativas: para de tentar
  - Solicitação marcada como RECUSADA
  - Pipeline marcado como FALHA_FINAL
  - Passageiro notificado com nenhum_motorista
  - Todos os registros de fila têm data_resposta

- [ ] **Persistência de Dados**
  - Todos os status salvos no BD
  - data_envio setada
  - data_resposta setada quando responde/timeout
  - Tentativa_atual incrementa corretamente
  - Nenhuma solicitação fica com status intermediário errado

### C. INTEGRAÇÃO COM SSE

- [ ] Motorista recebe SSE nova_solicitacao
  - Tem filaId? ✅
  - Tem solicitacaoId? ✅
  - Tem dados da corrida? ✅

- [ ] Motorista recebe SSE solicitacao_aceita
  - Quando aceita? ✅
  - Passageiro notificado? ✅

- [ ] Pipeline correto
  - Sem "logs fantasma"? ✅
  - Eventos no tempo certo? ✅

### D. TRATAMENTO DE ERROS

- [ ] Fila não encontrada
  - Exception lançada? ✅
  - Erro tratado? ✅

- [ ] Solicitação não encontrada
  - Exception lançada? ✅
  - Erro tratado? ✅

- [ ] Sem próximo motorista
  - Marca como RECUSADA? ✅
  - Notifica passageiro? ✅

- [ ] Sem SSE ativo
  - Log de aviso? ✅
  - Sistema continua mesmo sem notificar? ✅

### E. SEGURANÇA

- [ ] Autenticação nos endpoints
  - `/aceitar` requer token? ✅
  - `/recusar` requer token? ✅

- [ ] Motorista só vê suas notificações
  - Só recebe via SSE dele? ✅
  - Não fica vendo outras solicitações? ✅

---

## 🐛 PROBLEMAS CONHECIDOS A INVESTIGAR

### 1. Status Pendente Não Finaliza?
**Hipótese:** Quando limite de tentativas é atingido, a solicitação está marcada como RECUSADA mas não está sendo salva no BD corretamente.

**Como verificar:**
```sql
SELECT id, status_solicitacao, id_status_pipeline, tentativa_atual
FROM solicitacoes
WHERE id = 1;
-- Esperado: status = RECUSADA (3), pipeline = FALHA_FINAL
```

**Linhas de código a revisar:**
- `PassageRequestAutomaticService.java:376-382` - marcarSolicitacaoComoRecusada()
- `PassageRequestAutomaticService.java:364-374` - atualizarStatusPipeline()

### 2. Dados na Fila Não Finalizam?
**Hipótese:** Alguns registros ficam com data_resposta = NULL mesmo após timeout/timeout.

**Como verificar:**
```sql
SELECT id, id_status_fila, data_resposta
FROM solicitacao_fila_motoristas
WHERE id_solicitacao = 1;
-- Todos deveriam ter data_resposta preenchida
```

**Linhas de código a revisar:**
- `PassageRequestAutomaticService.java:289-308` - handleTimeoutMotorista()
- `PassageRequestAutomaticService.java:264-283` - handleMotoristaRecusa()

### 3. SSE Notifica Mesmo Sem Motorista Ativo?
**Hipótese:** Sistema tenta notificar mas motorista não tem SSE aberto.

**Comportamento esperado:** Log de aviso: "Nenhuma conexão SSE ativa"

**Linhas de código a revisar:**
- `SseNotificationService.java` - Check no método notificar()

### 4. Rejeitação Automática Não Funciona?
**Hipótese:** Quando motorista aceita, outros não são rejeitados automaticamente.

**Como verificar:**
```sql
SELECT id, id_motorista, id_status_fila
FROM solicitacao_fila_motoristas
WHERE id_solicitacao = 1;
-- Apenas 1 deve ter ACEITA, outros RECUSADA (rejeitados automaticamente)
```

**Linhas de código a revisar:**
- `PassageRequestAutomaticService.java:313-334` - rejeitarOutrosMotoristas()

### 5. Tentativa Atual Não Incrementa Corretamente?
**Hipótese:** tentativa_atual não incrementa em recusa/timeout.

**Como verificar:**
```sql
SELECT tentativa_atual FROM solicitacoes WHERE id = 1;
-- Deveria ir de 0 → 1 → 2 → 3 (depois para)
```

**Linhas de código a revisar:**
- `PassageRequestAutomaticService.java:204-207` - incremento tentativa

---

## 📋 ARQUIVOS PRINCIPAIS A REVISAR

```
src/main/java/com/example/fatecCarCarona/

├── controller/
│   ├── PassageRequestResponseController.java (NOVO - endpoint aceitar/recusar)
│   └── NotificacaoController.java (SSE stream)
│
├── service/
│   ├── PassageRequestAutomaticService.java (CORE - fluxo automático)
│   │   ├── iniciarFluxoAutomatico()
│   │   ├── criarFilaMotoristas()
│   │   ├── enviarProximoMotorista()
│   │   ├── handleMotoristaAceita() ⭐ CRÍTICO
│   │   ├── handleMotoristaRecusa() ⭐ CRÍTICO
│   │   ├── handleTimeoutMotorista()
│   │   ├── rejeitarOutrosMotoristas()
│   │   ├── marcarSolicitacaoComoRecusada() ⭐ CRÍTICO
│   │   └── atualizarStatusPipeline()
│   │
│   ├── SseNotificationService.java (notificações em tempo real)
│   │   └── notificar()
│   │
│   └── SecurityConfig.java (segurança e autenticação)
│
└── repository/
    └── PassageRequestQueueRepository.java (queries customizadas)
```

---

## 🧪 TESTES A VALIDAR

```
src/test/java/com/example/fatecCarCarona/

├── integration/
│   └── PassageRequestAutomaticE2ETest.java
│       ├── testIniciarFluxoAutomatico_Sucesso
│       ├── testMotoristaAceita
│       ├── testMotoristaRecusa
│       ├── testTimeoutMotorista
│       └── testLimiteTentativas
│
└── service/
    └── PassageRequestAutomaticServiceTest.java
        ├── testIniciarFluxoAutomatico_CriaFila
        ├── testEnviarProximoMotorista_*
        ├── testHandleMotoristaAceita_*
        └── testHandleMotoristaRecusa_*
```

---

## 📝 FORMATO DE RESPOSTA ESPERADO

Ao encontrar problemas, documentar assim:

```markdown
### ❌ Problema [Número]: [Título Descritivo]

**Severidade:** CRÍTICO | ALTO | MÉDIO | BAIXO

**Localização:** 
- Arquivo: `PassageRequestAutomaticService.java`
- Linhas: 200-210
- Método: `enviarProximoMotorista()`

**Descrição:**
[Explicação clara do que está errado e por quê]

**Comportamento Atual:**
[O que acontece agora - código ou comportamento observado]

**Comportamento Esperado:**
[O que deveria acontecer conforme a regra de negócio]

**Impacto:**
[Qual é o impacto disso para o usuário/sistema]

**Exemplo de Falha:**
[Se possível, descrever cenário where isso acontece]

**Sugestão de Correção:**
[Código ou lógica proposta para corrigir]

**Relacionado com:**
[Outros problemas ou métodos relacionados]
```

---

## 🎯 OBJETIVO FINAL

Ao terminar a validação, fornecer:

1. ✅ **Lista de todos os problemas encontrados** com severidade
2. ✅ **Análise do impacto** de cada problema
3. ✅ **Sugestões de correção** específicas
4. ✅ **Ordem de prioridade** para correção
5. ✅ **Testes que devem passar** após correção
6. ✅ **Confirmação se o fluxo está pronto** para produção

---

## 📞 Perguntas de Verificação Final

After all validations, answer:

- [ ] O fluxo está **100% correto** conforme a regra de negócio?
- [ ] Todos os **status são persistidos** corretamente no BD?
- [ ] **SSE funciona** sem erros?
- [ ] **Segurança** está adequada?
- [ ] **Tratamento de erros** é robusto?
- [ ] **Testes cobrem** todos os casos?
- [ ] Código está **pronto para produção**?

---

**Tempo Estimado:** 2-3 horas
**Criticidade:** ALTA - Refatoração Core do Sistema
**Data de Validação:** [será preenchida]
**Validador:** [seu nome]


