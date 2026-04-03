# 🚗 FatecRide — Backend

API REST do sistema de caronas da Fatec, com autenticação JWT, gestão de usuários, veículos, caronas e agendamento recorrente.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Configuração do Ambiente](#configuração-do-ambiente)
- [Como Executar](#como-executar)
- [Autenticação](#autenticação)
- [Agendamento de Caronas](#agendamento-de-caronas)
- [Endpoints](#endpoints)
- [Fluxo de Teste (Insomnia)](#fluxo-de-teste-insomnia)
- [Observações Técnicas](#observações-técnicas)
- [Swagger / OpenAPI](#swagger--openapi)
- [Autores](#autores)

---

## Visão Geral

O FatecRide Backend fornece a infraestrutura para:

- Cadastro e autenticação de usuários (motorista / passageiro)
- Gerenciamento de perfil, endereço e veículos
- Criação, atualização e finalização de caronas
- Solicitação de carona por passageiros
- Agendamento recorrente de caronas (por dia da semana ou intervalo de dias)
- Scheduler diário que materializa automaticamente as caronas agendadas

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.0 |
| Spring Security | — |
| Spring Data JPA / Hibernate | — |
| Spring WebFlux | — |
| MySQL | 8.x |
| JWT (`com.auth0:java-jwt`) | 4.4.0 |
| Lombok | — |
| Springdoc OpenAPI (Swagger) | 2.8.9 |

---

## Estrutura do Projeto

```
src/main/java/com/example/fatecCarCarona/
├── controller/     → Endpoints REST
├── service/        → Regras de negócio
├── repository/     → Acesso a dados (Spring Data JPA)
├── entity/         → Entidades do banco de dados
├── dto/            → Contratos de entrada e saída
├── scheduler/      → Jobs agendados (cron diário)
├── config/         → Configurações gerais
└── infra/          → Segurança, filtros e infraestrutura
```

Classe principal: `FatecCarCaronaApplication.java` (anotada com `@EnableScheduling`)

---

## Configuração do Ambiente

### Pré-requisitos

- Java 21
- MySQL 8.x rodando localmente
- Maven (ou use o wrapper `mvnw`)

> ⚠️ **Atenção ao nome do banco:** use sempre **letras minúsculas** para evitar incompatibilidades entre sistemas operacionais. O MySQL no Windows é case-insensitive, mas no Linux não é.

### 1. Criar o banco de dados

No MySQL Workbench ou terminal, execute:

```sql
CREATE DATABASE backendfatecarona;
```

### 2. Configurar o `application.properties`

Arquivo em `src/main/resources/application.properties`:

```properties
spring.application.name=fatecCarCarona

spring.datasource.url=jdbc:mysql://localhost:3306/backendfatecarona
spring.datasource.username=root
spring.datasource.password=root

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

api.security.token.secret=my-secret-key-from-video
```

> 🔒 **Em produção:** substitua as credenciais e o secret JWT por variáveis de ambiente. Nunca comite senhas reais no repositório.

---

## Como Executar

### Windows (PowerShell)

```powershell
# Build
.\mvnw.cmd clean install -DskipTests

# Rodar
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw clean install -DskipTests
./mvnw spring-boot:run
```

A API sobe em: **`http://localhost:8080`**

> ⚠️ **Problema com espaço no caminho (Windows):** se o seu usuário do Windows tiver espaço no nome (ex: `C:\Users\Felipe S\`), o Maven pode falhar. Nesse caso, mova o projeto para um caminho sem espaços (ex: `C:\DEV\FatecRide\`) e configure o repositório `.m2` também fora da pasta do usuário em `File > Settings > Maven > Local repository` no IntelliJ.

---

## Autenticação

A API usa **JWT (Bearer Token)**. O token é obtido no login e deve ser enviado em todas as requisições protegidas:

```http
Authorization: Bearer <token>
```

**Obter token:**
```http
POST /users/login
Content-Type: application/json

{
  "email": "usuario@fatec.sp.gov.br",
  "password": "senha123"
}
```

---

## Agendamento de Caronas

### Por dia da semana

```http
POST /agendar-ride-dia-semana
Authorization: Bearer <token>
Content-Type: application/json

{
  "ride": 10,
  "dia_semana_agendamento": [1, 3, 5]
}
```

> Os IDs dos dias da semana podem ser consultados em `GET /dias-semanas`.

### Por intervalo de dias

```http
POST /agendar-compromisso-intervalo-dias
Authorization: Bearer <token>
Content-Type: application/json

{
  "ride": 10,
  "dataInicio": "2026-04-03",
  "intervalo_dias": 2
}
```

> Os intervalos disponíveis podem ser consultados em `GET /intervalos-dias`.

### Scheduler automático

Um job com cron diário (`00:00`) em `SchedulerService`:
- Busca todos os agendamentos ativos
- Cria uma nova carona copiando os dados da carona base
- Atualiza o status dos agendamentos quando necessário

---

## Endpoints

### Públicos (sem autenticação)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/users/criarMotorista` | Cadastrar motorista |
| POST | `/users/criarPassageiro` | Cadastrar passageiro |
| POST | `/users/login` | Autenticar e obter token |
| GET | `/userType` | Listar tipos de usuário |
| GET | `/courses` | Listar cursos |
| GET | `/genders` | Listar gêneros |
| GET | `/states` | Listar estados |
| GET | `/states/{id}` | Buscar estado por ID |
| GET | `/cities/{id_estado}` | Listar cidades do estado |
| GET | `/cep/{cep}` | Consultar endereço por CEP |

### Protegidos (requer `Authorization: Bearer <token>`)

#### Usuário
| Método | Rota | Descrição |
|---|---|---|
| GET | `/users` | Buscar dados do usuário logado |
| PUT | `/users` | Atualizar dados do usuário |
| DELETE | `/users` | Deletar conta |

#### Endereço
| Método | Rota | Descrição |
|---|---|---|
| GET | `/address` | Buscar endereço do usuário |
| PUT | `/address/{idAddres}` | Atualizar endereço |

#### Veículos
| Método | Rota | Descrição |
|---|---|---|
| GET | `/veiculos` | Listar veículos do motorista |
| GET | `/veiculos/{id}` | Buscar veículo por ID |
| POST | `/veiculos` | Cadastrar veículo |
| PUT | `/veiculos/{id}` | Atualizar veículo |
| DELETE | `/veiculos/{id}` | Remover veículo |

#### Caronas
| Método | Rota | Descrição |
|---|---|---|
| POST | `/rides` | Criar carona |
| GET | `/rides/corridasAtivas` | Listar caronas ativas |
| GET | `/rides/concluidas` | Listar caronas concluídas |
| PUT | `/rides/{rideId}` | Atualizar carona |
| GET | `/rides/requestsForMyRide` | Ver solicitações recebidas |
| PUT | `/rides/{idSolicitacao}/acept` | Aceitar solicitação |
| PUT | `/rides/finalizar/{rideId}` | Finalizar carona |

#### Solicitações
| Método | Rota | Descrição |
|---|---|---|
| POST | `/solicitacao/proximos` | Buscar caronas próximas |
| POST | `/solicitacao` | Solicitar carona |
| PUT | `/solicitacao/cancelar/{id}` | Cancelar solicitação |
| GET | `/solicitacao/concluidas` | Listar solicitações concluídas |
| GET | `/solicitacao/pending` | Listar solicitações pendentes |

#### Agendamento
| Método | Rota | Descrição |
|---|---|---|
| GET | `/dias-semanas` | Listar dias da semana disponíveis |
| GET | `/intervalos-dias` | Listar intervalos disponíveis |
| POST | `/agendar-ride-dia-semana` | Agendar por dia da semana |
| PUT | `/agendar-ride-dia-semana/desativar/{id}` | Desativar agendamento por dia |
| POST | `/agendar-compromisso-intervalo-dias` | Agendar por intervalo |
| PUT | `/agendar-compromisso-intervalo-dias/desativar/{id}` | Desativar agendamento por intervalo |

#### Geolocalização
| Método | Rota | Descrição |
|---|---|---|
| GET | `/local?local=<texto>` | Buscar localização por texto |

---

## Fluxo de Teste (Insomnia)

Sequência recomendada para testar o fluxo completo:

1. **Login** → `POST /users/login` → salvar o `token` retornado
2. **Listar veículos** → `GET /veiculos` (com token)
3. **Criar carona** → `POST /rides` (com token)
4. **Buscar caronas ativas** → `GET /rides/corridasAtivas` → anotar o `id` da carona
5. **Consultar catálogos** → `GET /dias-semanas` e `GET /intervalos-dias`
6. **Criar agendamento** → `POST /agendar-ride-dia-semana` ou `POST /agendar-compromisso-intervalo-dias`
7. **Validar** → após a execução do scheduler diário (00:00), verificar as novas caronas geradas em `/rides/corridasAtivas`

---

## Observações Técnicas

- **Scheduler:** o job diário em `SchedulerService` é responsável por criar as caronas recorrentes. Em ambiente de desenvolvimento, pode ser necessário ajustar o horário do cron para testar.
- **Tabela `origens`:** a entidade `Origin` é mapeada nessa tabela com os campos `id_origem`, `id_cidade`, `logradouro`, `numero`, `bairro`, `cep`, `latitude` e `longitude`.
- **Dados de domínio:** em um banco recém-criado, algumas funcionalidades dependem de registros base nas tabelas auxiliares (`dia_semana`, `intervalo_dias`, `status_carona`, `tipo_usuario`, etc.). Certifique-se de populá-las antes de testar.
- **Convenção de nomes:** sempre use letras minúsculas para o nome do banco (`backendfatecarona`) e mantenha consistente entre todos os membros do time para evitar erros de conexão.

---

## Swagger / OpenAPI

Com a API em execução, acesse a documentação interativa:

- **Interface:** `http://localhost:8080/swagger-ui/index.html`
- **JSON:** `http://localhost:8080/v3/api-docs`

---

## Autores

**Equipe FatecRide**

| Nome | GitHub |
|---|---|
| Felipe SMZ | [@Felipe-SMZ](https://github.com/Felipe-SMZ) |
| Marcos Santos | [@MarcosVVSantos](https://github.com/MarcosVVSantos) |
| Guilherme Rufino | [@rufinoguilherme633](https://github.com/rufinoguilherme633) |