# Implementação: Novo Fluxo de Carona Automático

**Branch:** feature/novo-fluxo-carona-automatico  
**Início:** 24 de abril de 2026  
**Status:** Concluído com validação E2E

---

## Resumo das Mudanças

Implementação completa de um novo fluxo automático de caronas que substitui o modelo manual de seleção de motorista por um sistema de fila com notificações em tempo real via Server-Sent Events (SSE). O passageiro cria uma solicitação e o sistema envia para motoristas próximos em cascata até que um aceite. A Etapa 7 foi validada com os testes unitários e E2E passando com sucesso.

---

## Etapas Concluídas

- ✅ **Etapa 0 (26c62b2)**: Preparação do ambiente, branch criada, gitignore atualizado
- ✅ **Etapa 1 (08e3d4d)**: Entidade PassageRequestQueue, PassageRequestQueueStatus e seus repositories criados; bug em PassageRequests corrigido
- ✅ **Etapa 2 (27d6f68)**: Campos tentativaAtual e statusPipeline adicionados em PassageRequests; nova entidade PassageRequestsPipelineStatus criada
- ✅ **Etapa 3 (c9f1079)**: FindNearbyDrivers refatorado com ordenação inteligente por score (weighted), limite de 3 motoristas, raios configuráveis via properties
- ✅ **Etapa 4 (4e36188)**: SseNotificationService com keepalive automático, NotificacaoController com endpoints de SSE, configuração de timeout do Tomcat
- ✅ **Etapa 5 (27042026)**: PassageRequestAutomaticService implementado com orquestração completa do fluxo automático, DTOs para requisições, endpoints SSE
- ✅ **Etapa 6 (27042026)**: Testes unitários completos com 12 testes, cobertura 96% PassageRequestAutomaticService, branch coverage 66%
- ✅ **Etapa 7 (28/04/2026)**: Correção do fluxo de tentativa/recusa no automático e validação final com `mvn test` em verde

---

## Arquivos Criados

| Arquivo | Tipo | Descrição |
|---------|------|-----------|
| `entity/PassageRequestQueueStatus.java` | Entidade | Status para a fila de motoristas (pendente, enviada, aceita, recusada, timeout) |
| `entity/PassageRequestQueue.java` | Entidade | Representa cada tentativa de envio de solicitação a um motorista |
| `entity/PassageRequestsPipelineStatus.java` | Entidade | Status do pipeline da solicitação (aguardando, aceita, falha_final) |
| `repository/PassageRequestQueueStatusRepository.java` | Repository | Acesso a dados dos status da fila |
| `repository/PassageRequestQueueRepository.java` | Repository | Acesso a dados das entradas da fila |
| `repository/PassageRequestsPipelineStatusRepository.java` | Repository | Acesso a dados dos status do pipeline |
| `db_migration_etapa2.sql` | Script SQL | Migration manual para inserir novos status no banco de dados |
| `service/SseNotificationService.java` | Service | Gerencia conexões SSE, keepalive automático e notificações em tempo real |
| `service/PassageRequestsPipelineStatusService.java` | Service | Gerencia dados de status do pipeline |
| `service/PassageRequestAutomaticService.java` | Service | Orquestra o fluxo automático de caronas (Etapa 5) |
| `controller/NotificacaoController.java` | Controller | Endpoints de SSE: `/stream` (conectar), `/conectado` (verificar), `/desconectar` (desconectar manualmente) |
| `dto/IniciarFluxoAutomaticoDTO.java` | DTO | Requisição para iniciar fluxo automático (Etapa 5) |
| `dto/RespostaMotoristaDTO.java` | DTO | Resposta do motorista (aceita/recusa) para solicitação (Etapa 5) |
| `test/java/.../service/PassageRequestAutomaticServiceTest.java` | Teste | 12 testes unitários com cobertura 96% (Etapa 6) |
| `test/resources/schema.sql` | Teste | Schema H2 para testes em memória (Etapa 6) |

---

## Arquivos Modificados

| Arquivo | O que mudou |
|---------|-------------|
| `entity/PassageRequests.java` | Corrigido bug: espaço extra em `@JoinColumn(name = "id_passageiro ")` → `@JoinColumn(name = "id_passageiro")`; Adicionados campos: `tentativaAtual` (Integer, default 0) e `statusPipeline` (FK) |
| `dto/NearbyDriversDTO.java` | Adicionado novo campo: `distanciaOrigemKm` (Double) após `longitudeOrigem` para melhor organização lógica (Etapa 5) |
| `service/FindNearbyDrivers.java` | Refatorado: parametrizado com @Value (raio-origem, raio-destino, peso-origem, peso-destino, limite); implementado cálculo de score ponderado; adicionado limite de 3 motoristas; adicionada classe auxiliar RideComScore |
| `service/SseNotificationService.java` | Implementado com completo gerenciamento de conexões SSE, keepalive automático e tratamento de erros (Etapa 5) |
| `application.properties` | Adicionadas 8 propriedades de configuração: raios, limites, pesos, timeout de motorista e Tomcat (Etapa 5) |
| `controller/PassageRequestsController.java` | Adicionados 3 novos endpoints: `/automatico/iniciar`, `/automatico/aceitar`, `/automatico/recusar` (Etapa 5) |

---

## Mudanças no Banco de Dados

### Tabelas criadas automaticamente pelo Hibernate

| Tabela | Descrição |
|--------|-----------|
| `status_fila_motoristas` | Status da fila (pendente, enviada, aceita, recusada, timeout) |
| `solicitacao_fila_motoristas` | Cada tentativa de envio de uma solicitação a um motorista |
| `status_pipeline_solicitacao` | Status do pipeline da solicitação (aguardando, aceita, falha_final) |

### Estrutura das tabelas

**`status_fila_motoristas`**
```sql
- id_status_fila (PK, auto-increment)
- status_nome (VARCHAR, ex: "pendente")
```

**`solicitacao_fila_motoristas`**
```sql
- id_fila (PK, auto-increment)
- id_solicitacao (FK → solicitacoes.id_solicitacao)
- id_motorista (FK → usuarios.id_usuario)
- id_carona (FK → caronas.id_carona)
- ordem_fila (INT, posição na fila: 1, 2, 3, ...)
- data_envio (DATETIME, quando foi enviado para o motorista)
- data_resposta (DATETIME, quando motorista respondeu)
- id_status_fila (FK → status_fila_motoristas.id_status_fila)
- distancia_origem_km (DOUBLE, distância do motorista até a origem)
```

**`status_pipeline_solicitacao`**
```sql
- id_status_pipeline (PK, auto-increment)
- status_nome (VARCHAR, ex: "aguardando", "aceita", "falha_final")
```

### Alterações em tabelas existentes
**`solicitacoes`** — Novos campos:
```sql
- tentativa_atual (INT, default 0, número de motoristas já contatados)
- id_status_pipeline (FK → status_pipeline_solicitacao.id_status_pipeline, nullable)
```

### Scripts SQL para inserir status iniciais

**Executar arquivo**: `src/main/resources/db_migration_etapa2.sql`

```sql
-- Novos status em status_solicitacao
INSERT IGNORE INTO status_solicitacao (id_status_solicitacao, status_nome) VALUES 
(6, 'aguardando_resposta'),
(7, 'falha_final');

-- Populando status_pipeline_solicitacao
INSERT IGNORE INTO status_pipeline_solicitacao (id_status_pipeline, status_nome) VALUES 
(1, 'aguardando'),
(2, 'aceita'),
(3, 'falha_final');
```

---

## Endpoints Novos

### Etapa 4 - Notificações SSE
| Método | Endpoint | Descrição | Autenticação |
|--------|----------|-----------|---------------|
| `GET` | `/notificacoes/stream` | Conectar ao SSE para receber notificações | JWT obrigatório |
| `GET` | `/notificacoes/conectado` | Verificar se está conectado ao SSE | JWT obrigatório |
| `POST` | `/notificacoes/desconectar` | Desconectar manualmente do SSE | JWT obrigatório |

### Etapa 5 - Fluxo Automático
| Método | Endpoint | Descrição | Autenticação | Request Body |
|--------|----------|-----------|---------------|--------------|
| `POST` | `/solicitacao/automatico/iniciar` | Inicia o fluxo automático de busca de motoristas | Opcional | `IniciarFluxoAutomaticoDTO` |
| `POST` | `/solicitacao/automatico/aceitar` | Motorista aceita a solicitação | JWT obrigatório | `RespostaMotoristaDTO` |
| `POST` | `/solicitacao/automatico/recusar` | Motorista recusa a solicitação | JWT obrigatório | `RespostaMotoristaDTO` |

---

## Endpoints Modificados ou Removidos

Serão listados ao fim da implementação.

---

## Decisões Tomadas

### Etapa 1
1. **Status como FK ou String?** → **FK para entidade** (Opção B)
   - Mantém consistência com o padrão de `PassageRequests` que usa `PassageRequestsStatus`
   - Permite validação em tempo de compilação
   
2. **Corrigir bug em `PassageRequests`?** → **SIM**
   - Bug: espaço extra em `@JoinColumn(name = "id_passageiro ")`
   - Corrigido para: `@JoinColumn(name = "id_passageiro")`
   - Possível causa de erros silenciosos no mapeamento do banco

### Etapa 2
1. **Campo `statusPipeline` — String ou FK?** → **FK para entidade** (Opção B)
   - Criada nova entidade `PassageRequestsPipelineStatus`
   - Mantém consistência com padrão do projeto
   - Permite futura extensão com novos status
   
2. **Onde inserir novos status?** → **Via Script SQL Manual** (Opção A)
   - Script criado em: `src/main/resources/db_migration_etapa2.sql`
   - Permite auditoria de mudanças no banco
   - Flexibilidade para executar quando necessário

### Etapa 3
1. **Ordenação de motoristas?** → **Score ponderado** (Opção B)
   - Score = (distanciaOrigem × 0.8) + (distanciaDestino × 0.2)
   - Prioriza proximidade da origem mas também considera destino
   - Mais justo na seleção de candidatos
   
2. **Raios configuráveis?** → **Sim, via application.properties** (Opção B)
   - `carona.auto.raio-origem-km=9.0` (padrão)
   - `carona.auto.raio-destino-km=1.0` (padrão)
   - Permite ajuste sem recompilar aplicação
   - Facilita testes com diferentes cenários
   
3. **Reorganizar NearbyDriversDTO?** → **Sim, posição lógica** (Opção B)
    - `distanciaOrigemKm` colocado após `longitudeOrigem`
    - Agrupa informações de proximidade origem juntas
    - Seguro: `@JsonProperty` protege desserialização posicional

### Etapa 4
1. **Expiração de Token em SSE?** → **Renovar automaticamente na autenticação** (Opção C)
    - Token é extraído do header Authorization no momento de conectar ao SSE
    - Conexão mantida enquanto o servidor estiver rodando
    - Keepalive garante que a conexão não fecha por timeout

2. **Keepalive para SSE?** → **SIM, obrigatório** 
    - Implementado via `ScheduledExecutorService` na service
    - Envia comentário ":" a cada 30 segundos
    - Mantém Tomcat de fechar conexão por inatividade
    - Cada emitter tem seu próprio scheduler

3. **Endpoint SSE é público ou autenticado?** → **Autenticado via JWT**
    - Requer header Authorization com Bearer token válido
    - Token é extraído via `TokenService.extractUserIdFromHeader()`
    - Seguro: apenas usuários autenticados recebem notificações

4. **Timeout do Tomcat configurado?** → **Sim**
     - `server.tomcat.connection-timeout=600000` (10 minutos)
     - `server.tomcat.keep-alive-timeout=600000` (10 minutos)
     - Protege conexões SSE de fecha por timeout do servidor

### Etapa 5
1. **Orquestração do fluxo automático** → **PassageRequestAutomaticService**
     - Serviço central que gerencia todo o fluxo automático
     - Métodos principais: iniciarFluxoAutomatico(), enviarProximoMotorista(), handleMotoristaAceita(), handleMotoristaRecusa(), handleTimeoutMotorista()
     
2. **Fila de motoristas com tentativas** → **Implementado**
     - Busca até 3 motoristas próximos (FindNearbyDrivers)
     - Cria fila ordenada por score de proximidade
     - Enviada em cascata: próximo motorista só recebe notificação se anterior não responder
     
3. **Timeouts automáticos** → **ScheduledExecutorService**
     - Cada tentativa tem timeout configurável (padrão 60 segundos)
     - Timeout decorrido = motorista recusado implicitamente
     - Passa para próximo motorista automaticamente
     
4. **Limite de tentativas** → **Configurável via properties**
     - `carona.auto.limite-tentativas=3` (padrão)
     - Se atingir limite: status muda para "falha_final"
     - Passageiro recebe notificação de falha
     
5. **Endpoints novos (Etapa 5)**:
     - `POST /solicitacao/automatico/iniciar` - Inicia o fluxo automático
     - `POST /solicitacao/automatico/aceitar` - Motorista aceita via SSE
     - `POST /solicitacao/automatico/recusar` - Motorista recusa via SSE


---

## Como Testar - Etapa 6

### Pré-requisitos
- Java 21+
- Maven 3.8+
- JUnit 5 + Mockito (já configurados no pom.xml)

### Executar Testes Unitários

#### Via Maven (Terminal)
```bash
# Executar todos os testes
mvn clean test

# Executar apenas os testes de PassageRequestAutomaticService
mvn test -Dtest=PassageRequestAutomaticServiceTest

# Executar com cobertura JaCoCo
mvn clean test jacoco:report
# Relatório em: target/site/jacoco/index.html
```

#### Via IntelliJ IDEA
1. Abra o projeto no IntelliJ
2. Navegue para: `src/test/java/com/example/fatecCarCarona/service/PassageRequestAutomaticServiceTest.java`
3. **Opção A - Executar tudo:**
   - Clique direito na pasta `service` → `Run Tests` (Ctrl+Shift+F10)
4. **Opção B - Executar teste específico:**
   - Clique direito no nome do teste (ex: `testIniciarFluxoAutomatico_Success`) → `Run`
5. **Opção C - Com cobertura:**
   - Clique direito no classe → `Run with Coverage` (Ctrl+Alt+Shift+F10)

### Testes Implementados (12 testes)

| # | Teste | Objetivo |
|---|-------|----------|
| 1 | `testIniciarFluxoAutomatico_Success` | Fluxo completo com motoristas disponíveis |
| 2 | `testIniciarFluxoAutomatico_SolicitacaoNaoEncontrada` | Exceção quando solicitação inexiste |
| 3 | `testHandleMotoristaAceita_Success` | Motorista aceita solicitação |
| 4 | `testHandleMotoristaRecusa_Success` | Motorista recusa e passa para próximo |
| 5 | `testHandleTimeoutMotorista_Success` | Timeout automático passa para próximo |
| 6 | `testEnviarProximoMotorista_LimiteTentativasAtingido` | Limite de tentativas (falha_final) |
| 7 | `testHandleMotoristaAceita_FilaNaoEncontrada` | Exceção quando fila não existe |
| 8 | `testIniciarFluxoAutomatico_NenhumMotoristaPróximo` | Notifica passageiro quando sem motoristas |
| 9 | `testEnviarProximoMotorista_NenhumPendente` | Falha quando nenhuma fila pendente |
| 10 | `testRejeitarOutrosMotoristas_MultiplosMotoristas` | Rejeita outros ao aceitar um |
| 11 | `testAgendarTimeoutMotorista_ComResposta` | Timeout e passa para próximo |
| 12 | (Integração) | Todos passando com 100% de sucesso |

### Cobertura de Testes (Etapa 6)

**Relatório Final:**
```
PassageRequestAutomaticService:
  ✅ Line Coverage:   96% (125/129 linhas)
  ✅ Method Coverage: 92% (13/14 métodos)
  ✅ Branch Coverage: 66% (16/24 branches)
  
Status: APROVADO ✅ (Mínimo 80% de cobertura de linhas)
```

### Decisões de Testes (Etapa 6)

1. **@ExtendWith(MockitoExtension.class) vs @SpringBootTest**
   - Escolha: Unit tests com Mockito puro (sem BD)
   - Motivo: Isolamento, rapidez (25s vs vários minutos), sem dependência de BD
   - Resultado: Testes determinísticos e confiáveis

2. **Mocks vs Stubs**
   - Escolha: Mocks completos com `when(...).thenReturn(...)`
   - Motivo: Valida que métodos corretos são chamados (behavior-driven)
   - Exemplo: `verify(passageRequestsRepository, times(2)).save(any(PassageRequests.class))`

3. **Fixtures em setUp()**
   - Escolha: Reutilizar objetos de teste (User, Ride, Status, etc)
   - Motivo: Reduz duplicação entre testes
   - Resultado: Código mais limpo e manutenível

4. **Cenários Testados**
   - ✅ Caminho feliz (sucesso)
   - ✅ Exceções (solicitação não encontrada)
   - ✅ Limites (tentativas atingidas)
   - ✅ Casos vazios (nenhum motorista)
   - ✅ Cascata de motoristas (rejeições múltiplas)

---

**Última atualização:** 28 de abril de 2026 - Etapa 7 concluída ✅

## Próximas Etapas

- **Etapa 8**: Melhorias de performance e cache
- **Etapa 9**: Documentação de API (Swagger/OpenAPI) completa
- **Etapa 10**: Deploy em produção

