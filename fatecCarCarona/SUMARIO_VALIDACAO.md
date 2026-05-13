# 📊 SUMÁRIO EXECUTIVO - Validação do Fluxo Automático

## Instruções para o Agente

Você receberá este documento com a tarefa de validar completamente o fluxo automático de solicitação de carona. 

### Como Usar Este Documento

1. **Leia PROMPT_VALIDACAO_FLUXO.md** em sua totalidade
2. **Trace o código** seguindo o flow definido
3. **Verifique cada função** contra a regra de negócio
4. **Teste mentalmente** cada cenário
5. **Documente TODOS os problemas** encontrados
6. **Sugira correções** concretas

---

## 🗺️ Mapa Mental do Fluxo

```
                    PASSAGEIRO
                        ↓
        [1] Cria Solicitação (POST /solicitacao)
                        ↓
        [2] Inicia Fluxo (POST /solicitacao/automatico/iniciar)
                        ↓
                    SISTEMA
        ┌──────────────────────────────────────┐
        │ Busca motoristas próximos             │
        │ Cria fila de motoristas               │
        │ Envia 1ª notificação SSE              │
        └──────────────────────────────────────┘
                        ↓
        ┌───────────────┴───────────────┐
        ↓                               ↓
    MOTORISTA 1                    MOTORISTA 2
    Recebe notif                   (Espera)
        ↓
    [3] Responde em 60s
        ↓
    ┌───────────────┬───────────────┐
    ↓               ↓               ↓
  ACEITA        RECUSA          TIMEOUT
    (3A)          (3B)            (3C)
    ↓               ↓               ↓
  ✅             Próx Motorista  Próx Motorista
```

---

## 🔧 Funções Críticas a Revisar

| Função | Arquivo | Linhas | Crítico | Status |
|--------|---------|--------|---------|--------|
| `iniciarFluxoAutomatico()` | PassageRequestAutomaticService | 82-127 | 🔴 | ? |
| `criarFilaMotoristas()` | PassageRequestAutomaticService | 132-164 | 🟡 | ? |
| `enviarProximoMotorista()` | PassageRequestAutomaticService | 169-219 | 🔴 | ? |
| `handleMotoristaAceita()` | PassageRequestAutomaticService | 224-258 | 🔴 | ? |
| `handleMotoristaRecusa()` | PassageRequestAutomaticService | 263-283 | 🔴 | ? |
| `handleTimeoutMotorista()` | PassageRequestAutomaticService | 288-308 | 🔴 | ? |
| `rejeitarOutrosMotoristas()` | PassageRequestAutomaticService | 313-334 | 🟡 | ? |
| `marcarSolicitacaoComoRecusada()` | PassageRequestAutomaticService | 376-385 | 🔴 | ? |
| `atualizarStatusPipeline()` | PassageRequestAutomaticService | 365-374 | 🔴 | ? |

🔴 = Crítico | 🟡 = Importante | 🟢 = Menor Prioridade

---

## 📌 Anomalias Reportadas para Investigar

### Anomalia 1: Status PENDENTE Persiste
- **Problema:** Solicitação fica PENDENTE mesmo após timeout/limite de tentativas
- **Localização:** Fluxo de finalização (linhas 175-182)
- **Espera Status:** RECUSADA após limite atingido
- **SQL para Verificar:** 
  ```sql
  SELECT id, status_solicitacao FROM solicitacoes 
  WHERE id = 1 AND id_status_pipeline = 6; -- falha_final
  ```
- **Esperado:** status_solicitacao = 3 (RECUSADA)

### Anomalia 2: data_resposta NULL em Fila
- **Problema:** Alguns registros na fila ficam com data_resposta = NULL
- **Localização:** Métodos de resposta (aceitar, recusar, timeout)
- **Espera:** Sempre preenchido quando há resposta
- **SQL para Verificar:**
  ```sql
  SELECT id, id_status_fila, data_resposta 
  FROM solicitacao_fila_motoristas 
  WHERE data_resposta IS NULL AND id_status_fila IN (3,4,5);
  ```

### Anomalia 3: tentativa_atual Errado
- **Problema:** Contador não incrementa corretamente entre tentativas
- **Localização:** enviarProximoMotorista() linhas 204-207
- **Esperado:** 0 → 1 → 2 → 3 (para)
- **SQL para Verificar:**
  ```sql
  SELECT id, tentativa_atual, id_status_pipeline 
  FROM solicitacoes WHERE id = 1;
  ```

---

## 💡 Cenários de Teste Críticos

### Cenário A: Happy Path (DEVE PASSAR)
```
1. Passageiro cria solicitação → SQL: pendente
2. Passageiro inicia fluxo → SQL: aguardando (pipeline)
3. Motorista 1 recebe SSE
4. Motorista 1 aceita → SQL: aceita (tanto fila quanto solicitação)
5. Passageiro vê motorista confirmado

Verificações após:
- solicitacoes.status_solicitacao = 2 (ACEITA)
- solicitacoes.id_status_pipeline = 3 (ACEITA)
- solicitacao_fila_motoristas[motorista1].id_status_fila = 3 (ACEITA)
- solicitacao_fila_motoristas[motorista2,3,...].id_status_fila = 4 (RECUSADA)
- solicitacao_fila_motoristas[*].data_resposta ≠ NULL
```

### Cenário B: Recusa + Próximo (DEVE PASSAR)
```
1. Motorista 1 recusa
2. Sistema marca motorista 1 como recusada
3. Sistema envia para motorista 2
4. Motorista 2 aceita

Verificações após:
- solicitacao_fila_motoristas[motorista1].id_status_fila = 4 (RECUSADA)
- solicitacao_fila_motoristas[motorista2].id_status_fila = 3 (ACEITA)
- solicitação.status_solicitacao = 2 (ACEITA)
- solicitação.tentativa_atual = 2
```

### Cenário C: Limite Atingido (DEVE PASSAR)
```
1. Motorista 1 timeout → tentativa = 1
2. Motorista 2 timeout → tentativa = 2
3. Motorista 3 recusa → tentativa = 3
4. Sistema para (limite atingido)

Verificações após:
- solicitação.status_solicitacao = 3 (RECUSADA)
- solicitação.id_status_pipeline = 6 (FALHA_FINAL)
- solicitação.tentativa_atual = 3
- TODOS solicitacao_fila_motoristas[*].data_resposta ≠ NULL
- Passageiro notificado: "nenhum_motorista"
```

---

## 🔐 Requisitos de Segurança

- [ ] Endpoint `/aceitar` requer `Authentication`
- [ ] Endpoint `/recusar` requer `Authentication`
- [ ] Motorista só pode aceitar/recusar suas próprias notificações
- [ ] Token JWT validado em ambos endpoints
- [ ] SecurityConfig permite esses endpoints apenas para autenticados

---

## 🧪 Testes Unitários a Validar

```java
// PassageRequestAutomaticServiceTest.java

1. testInitialize()
   └─ Deve criar fila com todos motoristas

2. testEnviarProximoMotorista_Sucesso()
   └─ Deve enviar para 1º pendente

3. testHandleMotoristaAceita_Sucesso()
   └─ Fila = ACEITA, Solicitação = ACEITA, Outros rejeitados

4. testHandleMotoristaRecusa_ProximoDisponível()
   └─ Fila = RECUSADA, Enviar para próximo

5. testHandleTimeoutMotorista_ProximoDisponível()
   └─ Fila = TIMEOUT, Enviar para próximo

6. testEnviarProximoMotorista_LimiteTentativasAtingido()
   └─ Solicitação = RECUSADA, Pipeline = FALHA_FINAL, tentativa = 3

7. testLimiteTentativas()
   └─ Após 3 tentativas, para de tentar
```

---

## 📋 Checklist Pós-Validação

Ao terminar, responder:

- [ ] Todos os problemas foram documentados?
- [ ] Cada problema tem exemplo de como reproduzir?
- [ ] Cada problema tem sugestão de correção?
- [ ] Os cenários A, B, C funcionam conforme esperado?
- [ ] A segurança está validada?
- [ ] Os testes unitários passam?
- [ ] Não há código morto ou lógica duplicada?
- [ ] As transações são atômicas?
- [ ] O pipeline está claro e documentado?
- [ ] Pronto para code review?

---

## 📞 Próximos Passos

1. **Agora:** Você pode usar PROMPT_VALIDACAO_FLUXO.md para pedir validação
2. **Depois:** O agente fornecerá lista detalhada de problemas
3. **Então:** Corrigiremos os problemas encontrados
4. **Finalmente:** Testes unitários e integração validarão tudo

---

**Status:** ⚠️ EM VALIDAÇÃO
**Última Atualização:** 2026-05-06
**Versão:** 1.0-beta

