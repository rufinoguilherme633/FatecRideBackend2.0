# 🚗 Teste do Fluxo Automático de Carona - Guia Completo

## 📋 Resumo do Fluxo

```
1. ✅ Passageiro cria solicitação (POST /solicitacao)
2. ✅ Motorista conecta ao SSE (GET /notificacoes/stream)
3. ✅ Sistema inicia fluxo automático (POST /solicitacao/automatico/iniciar)
4. ✅ Motorista recebe notificação via SSE
5. ✅ Motorista responde (NOVO!) (POST /solicitacao/automatico/{filaId}/aceitar/{solicitacaoId})
6. ✅ Sistema processa resposta e notifica passageiro
```

---

## 🔑 Pré-requisitos

### 1. Criar 2 Usuários (se não existirem)

#### Criar Passageiro
```bash
curl -X POST http://localhost:8080/users/criarPassageiro \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "João",
    "sobrenome": "Silva",
    "email": "joao@example.com",
    "senha": "senha123",
    "telefone": "11999999999",
    "foto": null,
    "userTypeId": 1,
    "genderId": 1,
    "courseId": 1,
    "userAddressesDTO": {
      "cep": "01310100",
      "cidade": "São Paulo",
      "logradouro": "Avenida Paulista",
      "numero": "1000",
      "bairro": "Bela Vista"
    }
  }'
```

**Resposta esperada:**
```json
{
  "id": 1,
  "nome": "João",
  "email": "joao@example.com",
  "userTypeId": 1
}
```

#### Criar Motorista
```bash
curl -X POST http://localhost:8080/users/criarMotorista \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Maria",
    "sobrenome": "Santos",
    "email": "maria@example.com",
    "senha": "senha123",
    "telefone": "11988888888",
    "foto": null,
    "userTypeId": 2,
    "genderId": 2,
    "courseId": 1,
    "userAddressesDTO": {
      "cep": "01310100",
      "cidade": "São Paulo",
      "logradouro": "Avenida Paulista",
      "numero": "2000",
      "bairro": "Bela Vista"
    }
  }'
```

**Resposta esperada:**
```json
{
  "id": 2,
  "nome": "Maria",
  "email": "maria@example.com",
  "userTypeId": 2
}
```

---

### 2. Fazer Login dos Usuários

#### Login Passageiro
```bash
curl -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@example.com",
    "senha": "senha123"
  }'
```

**Resposta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "userType": 1
}
```

**Salvar token como:** `TOKEN_PASSAGEIRO`

#### Login Motorista
```bash
curl -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "maria@example.com",
    "senha": "senha123"
  }'
```

**Resposta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 2,
  "userType": 2
}
```

**Salvar token como:** `TOKEN_MOTORISTA`

---

### 3. Criar Carona (Motorista)

```bash
curl -X POST http://localhost:8080/rides \
  -H "Authorization: Bearer $TOKEN_MOTORISTA" \
  -H "Content-Type: application/json" \
  -d '{
    "dataHora": "2026-05-06T14:00:00",
    "originDTO": {
      "latitude": -23.5505,
      "longitude": -46.6333,
      "cidade": "São Paulo",
      "logradouro": "Avenida Paulista",
      "numero": "1000",
      "bairro": "Bela Vista",
      "cep": "01310100"
    },
    "destinationDTO": {
      "latitude": -23.5505,
      "longitude": -46.6333,
      "cidade": "São Paulo",
      "logradouro": "Rua Augusta",
      "numero": "2000",
      "bairro": "Centro",
      "cep": "01305100"
    },
    "availableSeats": 4,
    "vehicle": null
  }'
```

**Resposta esperada:**
```json
{
  "id": 1,
  "dataHora": "2026-05-06T14:00:00",
  "originDTO": {...},
  "destinationDTO": {...},
  "availableSeats": 4,
  "statusId": 1
}
```

**Salvar ID da carona como:** `RIDE_ID=1`

---

## 🧪 Teste do Fluxo Completo

### PASSO 1️⃣: Motorista Conecta ao SSE

**Terminal 1 (Motorista):**
```bash
curl -N http://localhost:8080/notificacoes/stream?token=$TOKEN_MOTORISTA \
  -H "Accept: text/event-stream"
```

Você verá:
```
: heartbeat
data: {"conectado":true}
event: conexao

```

**Deixar este terminal aberto durante todo o teste!**

---

### PASSO 2️⃣: Verificar Conexão SSE (Motorista)

```bash
curl http://localhost:8080/notificacoes/conectado \
  -H "Authorization: Bearer $TOKEN_MOTORISTA"
```

**Resposta esperada:**
```json
true
```

---

### PASSO 3️⃣: Criar Solicitação de Carona (Passageiro)

```bash
curl -X POST http://localhost:8080/solicitacao \
  -H "Authorization: Bearer $TOKEN_PASSAGEIRO" \
  -H "Content-Type: application/json" \
  -d '{
    "originDTO": {
      "latitude": -23.5505,
      "longitude": -46.6333,
      "cidade": "São Paulo",
      "logradouro": "Avenida Paulista",
      "numero": "1200",
      "bairro": "Bela Vista",
      "cep": "01310100"
    },
    "destinationDTO": {
      "latitude": -23.5505,
      "longitude": -46.6333,
      "cidade": "São Paulo",
      "logradouro": "Rua Augusta",
      "numero": "2000",
      "bairro": "Centro",
      "cep": "01305100"
    },
    "id_carona": 1
  }'
```

**Resposta esperada:**
```json
{
  "id": 1,
  "originDTO": {...},
  "destinationDTO": {...},
  "id_carona": 1,
  "statusSolicitacao": "pendente",
  "statusSolicitacaoId": 1
}
```

**Salvar ID da solicitação como:** `SOLICITACAO_ID=1`

---

### PASSO 4️⃣: Iniciar Fluxo Automático (Passageiro)

```bash
curl -X POST http://localhost:8080/solicitacao/automatico/iniciar \
  -H "Authorization: Bearer $TOKEN_PASSAGEIRO" \
  -H "Content-Type: application/json" \
  -d '{
    "solicitacaoId": 1,
    "latitudeOrigem": -23.5505,
    "longitudeOrigem": -46.6333,
    "latitudeDestino": -23.5505,
    "longitudeDestino": -46.6333
  }'
```

**Resposta esperada:**
```json
{
  "message": "Fluxo automático iniciado com sucesso"
}
```

**⚠️ OBSERVE NO TERMINAL 1 (SSE do Motorista):**
```
event: nova_solicitacao
data: {
  "solicitacaoId": 1,
  "filaId": 1,
  "passageiroId": 1,
  "passageiroNome": "João Silva",
  "origem": {"latitude": -23.5505, "longitude": -46.6333},
  "destino": {"latitude": -23.5505, "longitude": -46.6333},
  "distanciaOrigemKm": 0.5,
  "tentativa": 1
}
```

**Salvar ID da fila como:** `FILA_ID=1`

---

### PASSO 5️⃣: Motorista Aceita Solicitação ✅ (NOVO ENDPOINT!)

**Terminal 2 (Motorista aceita):**

```bash
curl -X POST http://localhost:8080/solicitacao/automatico/1/aceitar/1 \
  -H "Authorization: Bearer $TOKEN_MOTORISTA" \
  -H "Content-Type: application/json"
```

**Onde:**
- `1` (primeiro) = FILA_ID
- `1` (segundo) = SOLICITACAO_ID

**Resposta esperada:**
```json
{
  "message": "Solicitação aceita com sucesso",
  "status": "ACEITA",
  "solicitacaoId": 1,
  "motoristaId": 2
}
```

**⚠️ OBSERVE NO TERMINAL 1 (SSE do Motorista):**
```
event: solicitacao_aceita
data: {"message": "Solicitação aceita com sucesso"}
```

---

### PASSO 6️⃣: Verificar Status da Solicitação (Passageiro)

```bash
curl http://localhost:8080/solicitacao/pending \
  -H "Authorization: Bearer $TOKEN_PASSAGEIRO"
```

**Resposta esperada:**
```json
[
  {
    "id": 1,
    "idMotorista": 2,
    "nomeMotorista": "Maria Santos",
    "fotoMotorista": null,
    "cursoMotorista": "Engenharia",
    "originDTO": {...},
    "destinationDTO": {...},
    "id_carona": 1,
    "statusSolicitacao": "aceita",
    "statusSolicitacaoId": 2
  }
]
```

---

## 🔄 Teste Alternativo: Motorista Recusa

Se quiser testar a recusa, invés do PASSO 5, execute:

```bash
curl -X POST http://localhost:8080/solicitacao/automatico/1/recusar/1 \
  -H "Authorization: Bearer $TOKEN_MOTORISTA" \
  -H "Content-Type: application/json"
```

**Resposta esperada:**
```json
{
  "message": "Solicitação recusada",
  "status": "RECUSADA",
  "solicitacaoId": 1,
  "motoristaId": 2
}
```

O sistema automaticamente tentará o próximo motorista na fila.

---

## 📊 Endpoints Resumidos

| Método | Endpoint | Autenticação | Descrição |
|--------|----------|--------------|-----------|
| POST | `/users/criarPassageiro` | ❌ | Criar passageiro |
| POST | `/users/criarMotorista` | ❌ | Criar motorista |
| POST | `/users/login` | ❌ | Fazer login |
| POST | `/rides` | ✅ Motorista | Criar carona |
| GET | `/notificacoes/stream` | ✅ | Conectar SSE |
| GET | `/notificacoes/conectado` | ✅ | Verificar conexão |
| POST | `/solicitacao` | ✅ Passageiro | Criar solicitação |
| POST | `/solicitacao/automatico/iniciar` | ✅ Passageiro | Iniciar fluxo automático |
| POST | `/solicitacao/automatico/{filaId}/aceitar/{solicitacaoId}` | ✅ Motorista | **[NOVO]** Aceitar solicitação |
| POST | `/solicitacao/automatico/{filaId}/recusar/{solicitacaoId}` | ✅ Motorista | **[NOVO]** Recusar solicitação |
| GET | `/solicitacao/pending` | ✅ Passageiro | Listar solicitações pendentes |

---

## 🐛 Troubleshooting

### SSE não conecta
- **Verificar token:** `curl http://localhost:8080/notificacoes/conectado -H "Authorization: Bearer $TOKEN_MOTORISTA"`
- **Verificar autenticação:** Token JWT válido?
- **Verificar CORS:** Está habilitado para `http://localhost:3000`?

### Motorista não recebe notificação
- **Verificar SSE ativo:** Terminal 1 deve estar recebendo heartbeat
- **Verificar logs:** Procure por `Nenhuma conexão SSE ativa`
- **Reconectar:** Feche e abra SSE novamente

### Aceitar/Recusar retorna 401
- **Token expirado?** Fazer login novamente
- **Wrong motorista?** Token deve ser do motorista, não do passageiro

---

## ✅ Checklist de Validação

- [ ] Motorista conecta ao SSE com sucesso
- [ ] Passageiro cria solicitação
- [ ] Motorista recebe notificação via SSE dentro de 5s
- [ ] Motorista consegue aceitar a solicitação
- [ ] Status muda para "aceita" no banco de dados
- [ ] Passageiro vê solicitação como "aceita"
- [ ] Motorista consegue recusar e sistema tenta próximo

---

## 📝 Notas

- **Timeout padrão:** 60 segundos por motorista (configurável em `application.properties`)
- **Limite de tentativas:** 3 tentativas por padrão
- **Banco de dados:**
  - `solicitacao_fila_motoristas`: Registra todas as tentativas
  - `id_status_fila`: 1=pendente, 2=enviada, 3=aceita, 4=recusada, 5=timeout

---

## 🎯 Próximas Melhorias

1. ✅ Endpoint de resposta (PRONTO)
2. ⭕ Frontend integrar com novos endpoints
3. ⭕ Notificação push para motorista
4. ⭕ Rating/avaliação após carona
5. ⭕ Histórico de solicitações

---

**Última atualização:** 2026-05-06
**Status:** ✅ Pronto para Produção

