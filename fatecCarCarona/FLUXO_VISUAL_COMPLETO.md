# 🔄 Fluxo Automático - Diagrama Visual

## Fluxo Completo com Timing

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          FLUXO AUTOMÁTICO DE CARONA                          │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐                                          ┌──────────────┐
│  PASSAGEIRO  │                                          │   MOTORISTA  │
│   (João)     │                                          │   (Maria)    │
└──────────────┘                                          └──────────────┘
       │                                                         │
       │  [1] Conecta ao SSE                                     │
       │     GET /notificacoes/stream?token=...                │
       │     ✅ Retorna SseEmitter (stream aberto)             │
       │                                                         │
       │                                                    [2] Registra eventos SSE
       │                                                         │
       │  [3] Cria solicitação                                  │
       │     POST /solicitacao                                  │
       │     Body: { originDTO, destinationDTO, id_carona }     │
       │     ✅ Retorna: solicitacaoId = 1                     │
       │                                                         │
       │  [4] Inicia fluxo automático                           │
       │     POST /solicitacao/automatico/iniciar               │
       │     Body: { solicitacaoId, coords }                    │
       │     ✅ Sistema busca motoristas próximos              │
       │     ✅ Cria fila de motoristas                        │
       │                                                         │
       │                                    [5] SSE: nova_solicitacao
       │                                        filaId = 1
       │                                        ← Motorista vê no app!
       │                                         │
       │                                    [6] Usuário clica em "Aceitar"
       │                                         POST /solicitacao/automatico/
       │                                              1/aceitar/1
       │                                         Header: Authorization
       │                                         ✅ handleMotoristaAceita()
       │                                         │
       │                                         ├─ Atualiza fila status
       │                                         ├─ Atualiza solicitação
       │                                         └─ Rejeita outros motores
       │                                         │
       │  [7] SSE: solicitacao_aceita ←─────────┘
       │     Motorista: "Sua solicitação foi aceita!"
       │     Data: { motoristaId: 2, nome: "Maria" }
       │                                                         │
       │  [8] Verifica status                                   │
       │     GET /solicitacao/pending                           │
       │     ✅ Retorna com statusSolicitacao = "aceita"       │
       │                                                         │
       │     🎉 CARONA CONFIRMADA!                             │
       │                                                         │

```

---

## Sequência de Estados do Banco de Dados

```
TABELA: solicitacao_fila_motoristas (fila de motoristas)

┌──────┬───────────┬───────────┬────────────┬──────────────┐
│ id   │ motorista │ status    │ data_envio │ data_resposta│
├──────┼───────────┼───────────┼────────────┼──────────────┤
│ 1    │ 2 (Maria) │ enviada   │ 14:00:05   │ NULL         │  ← SSE ativo
└──────┴───────────┴───────────┴────────────┴──────────────┘
                           ↓
                   [Motorista clica Aceitar]
                           ↓
┌──────┬───────────┬───────────┬────────────┬──────────────┐
│ id   │ motorista │ status    │ data_envio │ data_resposta│
├──────┼───────────┼───────────┼────────────┼──────────────┤
│ 1    │ 2 (Maria) │ aceita    │ 14:00:05   │ 14:00:12     │  ✅ ACEITA
└──────┴───────────┴───────────┴────────────┴──────────────┘

TABELA: solicitacoes (solicitações de carona)

Antes:
┌───┬──────────┬───────────┬─────────┐
│ id│ passager │ carona    │ status  │
├───┼──────────┼───────────┼─────────┤
│ 1 │ 1 (João) │ 1 (Maria) │ aceita  │
└───┴──────────┴───────────┴─────────┘

```

---

## Casos de Uso

### ✅ Caso 1: Motorista Aceita (Happy Path)

```
Passageiro                      Motorista
    │                              │
    ├─ Cria solicitação            │
    ├─ Inicia fluxo               │
    │                              ├─ Recebe notificação via SSE
    │                              ├─ Clica em "Aceitar"  
    │                              ├─ POST .../aceitar/1
    │                              │  
    │                    ✅ Aceita │
    │◄─────────────────────────────┤
    │ Notificação: Motorista pronto│
    │ GET /solicitacao/pending     │
    │ Status: ACEITA ✅            │
    └──────────────────────────────┘
```

### ❌ Caso 2: Motorista Recusa (Tenta Próximo)

```
Passageiro                      Motorista 1
    │                              │
    ├─ Cria solicitação            │
    ├─ Inicia fluxo               │
    │                              ├─ Recebe notificação
    │                              ├─ Clica em "Recusar"
    │                              ├─ POST .../recusar/1
    │                              │
    │                    ❌ Recusa │
    │                        
    │     Status na fila: RECUSADA
    │     Sistema tenta próximo motorista
    │
    │                              Motorista 2
    │                              ├─ Recebe notificação
    │                              ├─ POST .../aceitar/1
    │                              │
    │                    ✅ Aceita │
    │◄─────────────────────────────┤
    │ Notificação: Motorista pronto│
    └──────────────────────────────┘
```

### ⏱️ Caso 3: Timeout (Motorista Não Responde)

```
Passageiro                      Motorista
    │                              │
    ├─ Inicia fluxo               │
    │                              ├─ Recebe notificação
    │                              │  (mas não responde)
    │
    │    [60 SEGUNDOS] ⏳
    │
    │    Timeout acionado
    │    Sistema tenta próximo motorista
    │
    │    ⏱️ handleTimeoutMotorista()
    │    Status na fila: TIMEOUT
    │
    │                              Motorista 2
    │                              ├─ Recebe notificação
    │                              ├─ POST .../aceitar/1
    │                              │
    │                    ✅ Aceita │
    │◄─────────────────────────────┤
    │ Motorista 1 é rejeitado auto │
    └──────────────────────────────┘
```

---

## Mapeamento de status_fila (Queue Status)

```
┌─────┬──────────────┬────────────────────────────────────────┐
│ ID  │ Nome         │ Descrição                              │
├─────┼──────────────┼────────────────────────────────────────┤
│ 1   │ PENDENTE     │ Aguardando ser enviado                 │
│ 2   │ ENVIADA      │ Notificação enviada ao motorista       │
│ 3   │ ACEITA       │ Motorista aceitou ✅                   │
│ 4   │ RECUSADA     │ Motorista recusou ❌                   │
│ 5   │ TIMEOUT      │ Motorista não respondeu ⏱️             │
└─────┴──────────────┴────────────────────────────────────────┘
```

---

## Mapeamento de status_solicitacao (Solicitation Status)

```
┌─────┬──────────────┬────────────────────────────────────────┐
│ ID  │ Nome         │ Descrição                              │
├─────┼──────────────┼────────────────────────────────────────┤
│ 1   │ PENDENTE     │ Aguardando resposta de algum motorista │
│ 2   │ ACEITA       │ Motorista aceitou ✅                   │
│ 3   │ RECUSADA     │ Nenhum motorista aceitou ❌            │
│ 4   │ CANCELADA    │ Passageiro cancelou                    │
│ 5   │ CONCLUÍDA    │ Corrida finalizada                     │
└─────┴──────────────┴────────────────────────────────────────┘
```

---

## Timeline Recomendada para Teste

```
T+0s      Passageiro cria solicitação
T+1s      Passageiro inicia fluxo automático
T+2s      ✅ Motorista recebe SSE evento
T+5s      Motorista clica em "Aceitar"
T+6s      ✅ Solicitação marcada como ACEITA
T+7s      Passageiro vê motorista confirmado
```

---

## Configurações Ajustáveis (application.properties)

```properties
# Timeout por motorista (em segundos)
carona.auto.timeout-motorista-segundos=60

# Limite de tentativas
carona.auto.limite-tentativas=3

# Intervalo de heartbeat SSE
carona.sse.heartbeat-interval-ms=30000
```

---

## 🔍 Como Debugar

### 1. Verificar logs do backend
```
grep "Motorista.*aceita\|recusa\|timeout" app.log
```

### 2. Consultar banco de dados
```sql
-- Ver fila de motoristas
SELECT id, id_motorista, id_status_fila, data_resposta 
FROM solicitacao_fila_motoristas 
WHERE id_solicitacao = 1;

-- Ver solicitação
SELECT id, id_passageiro, status_solicitacao 
FROM solicitacoes 
WHERE id = 1;
```

### 3. Verificar SSE ativo
```bash
curl http://localhost:8080/notificacoes/conectado \
  -H "Authorization: Bearer $TOKEN"
# Deve retornar: true
```

---

## 📱 Integração Frontend

Frontend deve fazer:

```javascript
// 1. Conectar SSE
const eventSource = new EventSource(
  '/notificacoes/stream?token=' + token,
  { headers: { 'Authorization': 'Bearer ' + token } }
);

// 2. Escutar nova_solicitacao
eventSource.addEventListener('nova_solicitacao', (e) => {
  const dados = JSON.parse(e.data);
  // Mostrar dialog com filaId e solicitacaoId
  showAcceptRejectDialog(dados.filaId, dados.solicitacaoId);
});

// 3. Botão "Aceitar"
async function aceitar(filaId, solicitacaoId) {
  const res = await fetch(
    `/solicitacao/automatico/${filaId}/aceitar/${solicitacaoId}`,
    {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token }
    }
  );
  const data = await res.json();
  console.log(data.message); // "Solicitação aceita com sucesso"
}

// 4. Botão "Recusar"
async function recusar(filaId, solicitacaoId) {
  const res = await fetch(
    `/solicitacao/automatico/${filaId}/recusar/${solicitacaoId}`,
    {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token }
    }
  );
  const data = await res.json();
  console.log(data.message); // "Solicitação recusada"
}
```

---

**Criado em:** 2026-05-06
**Versão:** 1.0
**Status:** ✅ Pronto para Teste

