# FatecRide Backend

Backend do sistema de caronas da Fatec com autenticação JWT, gerenciamento de usuários/veículos/caronas, agendamento recorrente e fluxo automático de matching com SSE.

## Sumário

- [Visão geral](#visao-geral)
- [Stack e arquitetura](#stack-e-arquitetura)
- [Setup local](#setup-local)
- [Execução](#execucao)
- [Autenticação](#autenticacao)
- [Novo fluxo de solicitação automática (SSE)](#novo-fluxo-de-solicitacao-automatica-sse)
- [Fluxo manual (legado)](#fluxo-manual-legado)
- [Endpoints principais](#endpoints-principais)
- [Teste E2E recomendado](#teste-e2e-recomendado)
- [Troubleshooting](#troubleshooting)
- [Swagger](#swagger)

## Visao geral

O projeto oferece:

- Cadastro e login de passageiro/motorista
- Cadastro e gestão de veículos e caronas
- Solicitação de carona com busca de motoristas próximos
- Encaminhamento automático da solicitação para motoristas em fila (com timeout)
- Notificações em tempo real via SSE
- Agendamento recorrente de caronas + scheduler diário

## Stack e arquitetura

- Java 21
- Spring Boot 3.5.0
- Spring Security + JWT (`com.auth0:java-jwt`)
- Spring Data JPA / Hibernate (MySQL)
- Spring Data MongoDB (módulo de apoio)
- Springdoc OpenAPI (Swagger)

Estrutura principal:

```text
src/main/java/com/example/fatecCarCarona/
  controller/   Endpoints REST
  service/      Regras de negócio
  repository/   Repositórios JPA
  entity/       Entidades
  dto/          Contratos de API
  scheduler/    Jobs agendados
  infra/        Segurança, tratamento de erro e infraestrutura
```

Classe principal: `src/main/java/com/example/fatecCarCarona/FatecCarCaronaApplication.java`

## Setup local

### Pré-requisitos

- Java 21
- MySQL 8+
- MongoDB local (porta padrão 27017)
- Maven (ou `mvnw`)

### Banco de dados

Crie o banco MySQL usado pelo seu `application.properties` (ajuste o nome conforme seu ambiente):

```sql
CREATE DATABASE backendfateccarona;
```

Configuração de referência (`src/main/resources/application.properties`):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/backendfatecCarona
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
spring.data.mongodb.uri=mongodb://localhost:27017/backendfateccarona
api.security.token.secret=trocar-em-producao
```

## Execucao

Windows PowerShell:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw clean compile
./mvnw spring-boot:run
```

API local: `http://localhost:8080`

## Autenticacao

Login:

```http
POST /users/login
Content-Type: application/json

{
  "email": "usuario@exemplo.com",
  "password": "senha"
}
```

Use o token em endpoints protegidos:

```http
Authorization: Bearer <token>
```

## Novo fluxo de solicitacao automatica (SSE)

Este e o fluxo principal da regra nova.

### 1) Motorista conecta no SSE

Endpoint:

- `GET /notificacoes/stream`

Pode enviar token por:

- header `Authorization: Bearer <token>`
- ou query param `?token=<jwt>` (útil em `EventSource` no navegador)

### 2) Passageiro cria solicitação

- `POST /solicitacao`

### 3) Passageiro inicia o fluxo automático

- `POST /solicitacao/automatico/iniciar`

Body (`IniciarFluxoAutomaticoDTO`):

```json
{
  "solicitacaoId": 27,
  "latitudeOrigem": -23.561414,
  "longitudeOrigem": -46.655881,
  "latitudeDestino": -23.568704,
  "longitudeDestino": -46.648242
}
```

Se coordenadas não forem enviadas, o backend usa origem/destino da solicitação persistida.

### 4) Backend envia evento SSE para o motorista da vez

Evento: `nova_solicitacao`

Payload atual (resumo):

```json
{
  "solicitacaoId": 27,
  "filaId": 4,
  "passageiroId": 54,
  "passageiroNome": "Nome Passageiro",
  "origem": { "latitude": -23.56, "longitude": -46.65 },
  "destino": { "latitude": -23.57, "longitude": -46.64 },
  "distanciaOrigemKm": 0.02,
  "tentativa": 1
}
```

### 5) Motorista responde

- Aceitar: `POST /solicitacao/automatico/aceitar`
- Recusar: `POST /solicitacao/automatico/recusar`

Body (`RespostaMotoristaDTO`):

```json
{
  "solicitacaoId": 27,
  "filaId": 4,
  "motoristaId": 49,
  "resposta": "aceita"
}
```

### Eventos de retorno ao passageiro

Dependendo do processamento:

- `solicitacao_aceita`
- `nenhum_motorista`
- `falha_final`

### Regra importante

No fluxo novo, o motorista deve saber da solicitação por SSE. O endpoint `GET /rides/requestsForMyRide` não e a fonte principal desse fluxo.

## Fluxo manual (legado)

Endpoints legados que ainda existem:

- `GET /rides/requestsForMyRide`
- `PUT /rides/{idSolicitacao}/acept`

Esse fluxo depende de associação direta com carona (`solicitacao.carona`), então o comportamento é diferente do fluxo automático.

## Endpoints principais

### Público

- `POST /users/criarMotorista`
- `POST /users/criarPassageiro`
- `POST /users/login`
- `GET /courses`, `GET /genders`, `GET /states`, `GET /cities/{id_estado}`, `GET /cep/{cep}`

### Motorista

- `POST /rides`
- `GET /rides/corridasAtivas`
- `GET /rides/concluidas`
- `PUT /rides/{rideId}`
- `PUT /rides/finalizar/{rideId}`

### Passageiro

- `POST /solicitacao/proximos`
- `POST /solicitacao`
- `GET /solicitacao/pending`
- `PUT /solicitacao/cancelar/{id_solicitacao}`

### Fluxo automático

- `POST /solicitacao/automatico/iniciar`
- `POST /solicitacao/automatico/aceitar`
- `POST /solicitacao/automatico/recusar`
- `GET /notificacoes/stream`
- `GET /notificacoes/conectado`
- `POST /notificacoes/desconectar`

## Teste E2E recomendado

Ordem recomendada para validar a regra nova:

1. Criar e logar 1 passageiro e 2+ motoristas
2. Motoristas conectam no SSE (`/notificacoes/stream`)
3. Motorista cria carona (`POST /rides`)
4. Passageiro cria solicitação (`POST /solicitacao`)
5. Passageiro inicia automático (`POST /solicitacao/automatico/iniciar`)
6. Validar recebimento do evento `nova_solicitacao` no motorista da vez
7. Aceitar ou recusar (`/solicitacao/automatico/aceitar|recusar`)
8. Validar retorno para passageiro em `/solicitacao/pending` e eventos SSE

## Troubleshooting

### 1) Erro de FK ao subir app (`id_status_carona`)

Erro típico:

`Cannot add or update a child row ... FK ... caronas.id_status_carona -> status_carona.id_status_carona`

Causa: registros em `caronas` com `id_status_carona` inválido (ex.: `0`).

Correção rápida:

```sql
SET SQL_SAFE_UPDATES = 0;

UPDATE caronas
SET id_status_carona = 1
WHERE id_carona > 0
  AND (id_status_carona = 0 OR id_status_carona IS NULL);

SET SQL_SAFE_UPDATES = 1;
```

### 2) SSE conecta e logo desconecta

Verifique:

- token válido no `Authorization` ou `?token=`
- frontend reconectando automaticamente em `onerror`
- se há exceção não tratada durante requests SSE

### 3) Motorista não recebe solicitação

Checklist:

- passageiro chamou `POST /solicitacao/automatico/iniciar`
- motorista estava conectado no SSE antes do envio
- fila foi criada com status `pendente/enviada`
- há motoristas/caronas elegíveis na busca de proximidade

## Swagger

- UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Autores

Equipe FatecRide:

- [Felipe SMZ](https://github.com/Felipe-SMZ)
- [Marcos Santos](https://github.com/MarcosVVSantos)
- [Guilherme Rufino](https://github.com/rufinoguilherme633)
