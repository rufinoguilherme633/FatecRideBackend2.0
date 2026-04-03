# 🚗 FatecRide — Plataforma de Caronas FATEC

Sistema completo de caronas universitárias desenvolvido para a FATEC, composto por três serviços independentes que trabalham em conjunto.

---

## Sumário

- [Visão Geral da Arquitetura](#visão-geral-da-arquitetura)
- [Serviços](#serviços)
- [Pré-requisitos](#pré-requisitos)
- [Como Executar o Projeto Completo](#como-executar-o-projeto-completo)
- [Convenções do Time](#convenções-do-time)
- [Autores](#autores)

---

## Visão Geral da Arquitetura

```
FatecRideBackend2.0/
├── fatecCarCarona/     → API principal (Java/Spring Boot) — porta 8080
├── backendNode/        → Serviço de anúncios (Node.js/Express) — porta 8081
└── mensagens/          → Serviço de mensagens em tempo real (Node.js + WebSocket) — porta 9000
```

Os três serviços compartilham o mesmo banco **MySQL** (`backendfatecarona`) para dados de usuários, e os serviços Node utilizam **MongoDB** para seus dados próprios.

```
Frontend (Mobile/Web)
        │
        ├──► fatecCarCarona (8080)  ──► MySQL
        ├──► backendNode    (8081)  ──► MongoDB + MySQL
        └──► mensagens      (9000)  ──► MongoDB + MySQL
```

---

## Serviços

### 🟢 fatecCarCarona — API Principal
**Tecnologia:** Java 21 + Spring Boot 3.5.0  
**Porta:** `8080`  
**Responsabilidades:**
- Cadastro e autenticação de usuários (motorista / passageiro)
- Gerenciamento de perfil, endereço e veículos
- Criação, atualização e finalização de caronas
- Solicitações de carona
- Agendamento recorrente de caronas (por dia da semana ou intervalo de dias)

📄 [Documentação completa → fatecCarCarona/README.md](./fatecCarCarona/README.md)  
📋 Swagger: `http://localhost:8080/swagger-ui/index.html`

---

### 🟡 backendNode — Serviço de Anúncios
**Tecnologia:** Node.js + Express 4 + MongoDB  
**Porta:** `8081`  
**Responsabilidades:**
- Cadastro e autenticação de anunciantes (empresas/estabelecimentos)
- Gerenciamento de anúncios
- Exibição de anúncios aleatórios para os usuários da plataforma

📄 [Documentação completa → backendNode/README.md](./backendNode/README.md)  
📋 Swagger: `http://localhost:8081/docs`

---

### 🔵 mensagens — Serviço de Mensagens
**Tecnologia:** Node.js + Express 4 + WebSocket + MongoDB  
**Porta:** `9000`  
**Responsabilidades:**
- Troca de mensagens em tempo real entre usuários via WebSocket
- Histórico de mensagens com paginação
- Controle de mensagens não lidas
- Gerenciamento de conversas

📄 [Documentação completa → mensagens/README.md](./mensagens/README.md)

---

## Pré-requisitos

Antes de rodar qualquer serviço, garanta que você tem instalado:

| Ferramenta | Versão mínima | Para qual serviço |
|---|---|---|
| Java (JDK) | 21 | fatecCarCarona |
| Maven (ou use o mvnw) | 3.x | fatecCarCarona |
| Node.js | 16+ | backendNode, mensagens |
| MySQL | 8.x | Todos |
| MongoDB | 6.x+ | backendNode, mensagens |

> ⚠️ **Windows:** evite caminhos com espaço no nome do usuário (ex: `C:\Users\Felipe S\`). Prefira trabalhar em `C:\DEV\` para evitar erros com Maven e npm.

---

## Como Executar o Projeto Completo

### 1. Banco de dados MySQL

Crie o banco antes de subir qualquer serviço:

```sql
CREATE DATABASE backendfatecarona;
```

> Use sempre **letras minúsculas** no nome do banco. O MySQL no Windows é case-insensitive, mas no Linux não — isso pode quebrar o ambiente de outros membros do time.

### 2. fatecCarCarona (Java) — porta 8080

```powershell
cd fatecCarCarona

# Windows (PowerShell)
.\mvnw.cmd clean install -DskipTests
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw clean install -DskipTests
./mvnw spring-boot:run
```

Configure `fatecCarCarona/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/backendfatecarona
spring.datasource.username=root
spring.datasource.password=root
api.security.token.secret=my-secret-key-from-video
```

### 3. backendNode (Node.js) — porta 8081

```bash
cd backendNode
npm install
```

Crie o arquivo `.env`:

```env
SECRET=sua_chave_jwt_forte_aqui
MONGODB_URI=mongodb://localhost/backendfatecnode
PORT=8081
NODE_ENV=development
```

```bash
npm start
```

### 4. mensagens (Node.js + WebSocket) — porta 9000

```bash
cd mensagens
npm install
```

Crie o arquivo `.env`:

```env
SECRET=mesma_chave_jwt_usada_nos_outros_servicos
MONGODB_URI=mongodb://localhost/chat
DATABASEMYSQL=backendfatecarona
USERMYSQL=root
PASSWORDMYSQL=root
PORT_REST=9000
NODE_ENV=development
```

```bash
node server.js
```

---

## Convenções do Time

Para evitar conflitos entre membros do time, siga sempre estas regras:

**Banco de dados**
- Nome do banco: sempre `backendfatecarona` (minúsculo, sem camelCase)
- Nunca comite alterações no `application.properties` com o nome do banco diferente

**Credenciais e secrets**
- Nunca comite senhas reais ou chaves JWT no repositório
- Use os valores de exemplo (`root`/`root`, `my-secret-key-from-video`) apenas em desenvolvimento local
- Em produção, use variáveis de ambiente

**Git**
- Trabalhe sempre em branches separadas por funcionalidade
- Nunca faça push direto na `main` sem Pull Request revisado
- Antes de abrir PR, teste localmente com os três serviços rodando

**Windows**
- Projetos devem ficar em caminhos sem espaços (ex: `C:\DEV\`)
- O repositório `.m2` do Maven deve estar fora de `C:\Users\<nome com espaço>\`

---

## Autores

**Equipe FatecRide**

| Nome | GitHub |
|---|---|
| Felipe SMZ | [@Felipe-SMZ](https://github.com/Felipe-SMZ) |
| Marcos Santos | [@MarcosVVSantos](https://github.com/MarcosVVSantos) |
| Guilherme Rufino | [@rufinoguilherme633](https://github.com/rufinoguilherme633) |