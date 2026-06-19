<div align="center">

## 💊 FarmaBook

**O caderno de encomendas e faltas da farmácia, agora digital.**

Sistema de gestão de encomendas, faltas, manipulações e receitas para farmácias.

[![CI](https://github.com/luizgabrielcb/farmabook/actions/workflows/ci.yml/badge.svg)](https://github.com/luizgabrielcb/farmabook/actions/workflows/ci.yml)
![Tests](https://img.shields.io/badge/tests-459%20passing-success)
![Coverage](https://img.shields.io/badge/coverage-94%25%20line-success)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)

</div>

## 📑 Sumário

- [Demonstração](#-demonstração)
- [O problema](#-o-problema)
- [A solução](#-a-solução)
- [Funcionalidades](#-funcionalidades)
- [Os domínios em detalhe](#-os-domínios-em-detalhe)
- [Arquitetura](#-arquitetura)
- [Stack tecnológica](#-stack-tecnológica)
- [Decisões de projeto que valem nota](#-decisões-de-projeto-que-valem-nota)
- [Testes e cobertura](#-testes-e-cobertura)
- [Começando](#-começando)
- [Executando em desenvolvimento](#-executando-em-desenvolvimento)
- [Como rodar na farmácia](#-como-rodar-na-farmácia)
- [API REST](#-api-rest)
- [Estrutura do repositório](#-estrutura-do-repositório)
- [Convenções de código](#-convenções-de-código)
- [Autor](#-autor)
---

## 📸 Demonstração

<img width="1919" height="999" alt="Captura de Tela 2026-06-17 às 20 46 37" src="https://github.com/user-attachments/assets/8ceecd25-479f-4d9b-b46b-0f152b887b94" />
<img width="1920" height="998" alt="Captura de Tela 2026-06-17 às 20 48 19" src="https://github.com/user-attachments/assets/5902b754-7b97-4771-807b-07edb589715c" />
<img width="1920" height="998" alt="Captura de Tela 2026-06-17 às 20 47 23" src="https://github.com/user-attachments/assets/c249564c-3f05-4106-8a8b-7c7a5660e06a" />

---

## 🩹 O problema

Este projeto nasceu de uma dor real, vivida na pele durante quase **4 anos trabalhando em uma farmácia**.

A farmácia tinha dois cadernos sagrados no balcão: o **caderno de encomendas** (o que o cliente pediu e a loja precisa comprar do distribuidor) e o **caderno de faltas** (o que faltou na prateleira e precisa ser reposto). E todo dia esses cadernos cobram seu preço:

- ✍️ **Anotação manual e propensa a erro** — na correria do balcão, é fácil esquecer de anotar uma encomenda, ou esquecer de **riscar** uma que já foi entregue. O caderno vira uma mistura de pedidos pendentes e resolvidos, sem clareza nenhuma.
- 🔍 **Buscar é um sofrimento** — quando um cliente volta **um mês depois** perguntando "o que mesmo eu encomendei?", começa a garimpagem página por página, tentando decifrar a letra de quem anotou.
- 🧑‍🤝‍🧑 **Ninguém sabe quem fez o quê** — quem anotou? quem pediu ao distribuidor? quem entregou? O caderno não responde. Quando algo dá errado, não há a quem perguntar.
- 📞 **Avisar o cliente é trabalhoso** — quando a encomenda chega, alguém precisa lembrar de ligar, digitar o número, escrever o recado... e muitas vezes simplesmente não acontece.
- 🗑️ **O histórico se perde** — caderno acaba, caderno se molha, caderno se perde. E com ele todo o histórico de quem comprou o quê.
## ✅ A solução

O **FarmaBook** substitui esses cadernos por um sistema web pensado para o balcão: rápido de operar, com **histórico permanente e pesquisável**, **rastreio de autoria** em cada passo e **notificação automática do cliente via WhatsApp**.

Cada encomenda e cada falta deixa de ser um rabisco e passa a ser um registro com **ciclo de vida bem definido** — sempre dá pra saber em que pé está cada pedido, quem cuidou dele e quando. Nada é apagado de fato; mesmo o que é "removido" continua no histórico. E quando o produto chega, um clique gera o link de WhatsApp com a mensagem pronta para o cliente.
 
---

## ✨ Funcionalidades

- 🧾 **Encomendas com itens independentes** — uma encomenda agrupa vários produtos, e cada item tem seu próprio ciclo de vida (`PENDENTE → PEDIDO → RECEBIDO → ENTREGUE`).
- 👣 **Rastreio de autoria em cada passo** — quem pediu ao distribuidor, quem recebeu, quem entregou e quando: tudo carimbado automaticamente com o usuário responsável.
- 💬 **WhatsApp automático na chegada** — ao marcar uma encomenda (ou manipulação) como recebida, o sistema monta um link `wa.me` com a mensagem pronta, saudação conforme o horário e número já normalizado.
- 💰 **Controle de pagamento por item** — incluindo o fluxo de "anotar na conta" (caderneta/fiado): `A PAGAR → ANOTAR → ANOTADO`, ou direto para `PAGO`.
- 📉 **Faltas de estoque** — registro do que faltou na prateleira, agrupável em pedidos de reposição por representante/distribuidora.
- 🔐 **Login por PIN** — operação ágil de balcão; cada ação registra automaticamente o operador.
- 🗂️ **Histórico permanente e à prova de renomeação** — nada é apagado de fato, e nomes de cliente/usuário/distribuidora ficam "congelados" no registro, então o passado sobrevive mesmo se um cadastro for renomeado.
---

## 🧩 Os domínios em detalhe

A análise abaixo reflete o modelo real do código (entidades, status e regras).

### 🧾 Encomendas (`order`)
O núcleo do sistema. Uma `Order` pertence a um cliente e reúne uma lista de `OrderItem`.

- **Item** → produto, categoria, quantidade, distribuidora, preço e status próprios.
- **Status do item** → `PENDING → ORDERED → RECEIVED → DELIVERED` (a ordem importa — é usada nos cálculos).
- **Status da encomenda** → calculado como o **menor status** entre os itens. Marcou todos como recebidos? A encomenda vira "recebida". Nunca é digitado à mão.
- **Carimbo de autoria** → cada transição grava o trio *quem / nome / quando* (`orderedBy…`, `receivedBy…`, `deliveredBy…`). Ao **regredir** um item (ex.: recebido → pedido), o carimbo correspondente é limpo.
- **Notificação** → na **primeira** vez que a encomenda atinge `RECEIVED`, uma notificação de WhatsApp é gerada e `notifiedAt` é preenchido.
- **Pagamento** → cada item tem status de pagamento (`TO_PAY → MAKE_NOTE → NOTED`, ou `PAID`); o da encomenda é o menor entre os itens. `NOTED` e `PAID` são terminais.
- **Transições em lote** (`mark-as-*` na encomenda) são **tolerantes**: itens inelegíveis são ignorados. As **individuais** (no item) são **estritas**: retornam `409 Conflict`.
- **Imutabilidade** → itens/encomendas `DELIVERED` não podem ser alterados, e uma encomenda com item entregue não pode ser excluída.
### 📉 Faltas (`shortage`)
`Shortage` registra um produto que faltou (produto, categoria, quantidade, preço de custo), com status `PENDING → ORDERED`. Várias faltas podem ser agrupadas em um `ShortageOrder` — um pedido de reposição direcionado a um **representante** e a uma **distribuidora**, também com ciclo `PENDING → ORDERED` e rastreio de autoria.

### 💬 Notificações (`notification`)
Toda notificação faz *snapshot* de telefone, nome, mensagem e link no momento do envio, e referencia a encomenda **ou** a manipulação que a originou. O link é um `wa.me` com a mensagem URL-encoded, saudação por horário (fuso `America/Sao_Paulo`) e telefone normalizado (só dígitos, prefixo `55`). Notificações podem ser **reenviadas** — o que gera um novo registro, preservando o original.

### 👤 Usuários e autenticação (`auth`)
`User` tem nome, PIN (hash **BCrypt**), papel (`ADMIN` / `SELLER`) e flag `active`. A autenticação é deliberadamente enxuta: cada requisição protegida envia o header `X-Auth-Pin`, e o `AuthService` casa o PIN contra os usuários ativos — o usuário resolvido é o "ator" carimbado na auditoria. Esse desenho é discutido em [Decisões de projeto](#-decisões-de-projeto-que-valem-nota).

### 🧱 Cadastros e base (`customer`, `distributor`, `catalog`, `shared`)
`Customer` (nome + telefone para WhatsApp), `Distributor` (nome) são os cadastros de apoio. `Category` é o enum de categorias de produto (`MEDICAMENTOS`, `PERFUMARIA`, `SUPLEMENTOS`, `PRODUTOS_NATURAIS`, `OUTROS`). Tudo herda de `Auditable` (`createdAt`, `updatedAt`, `deletedAt`).
 
---

## 🏛 Arquitetura

```
┌──────────────┐      HTTP /api      ┌──────────────┐      JDBC      ┌──────────────┐
│   Frontend   │ ──────────────────► │   Backend    │ ─────────────► │  PostgreSQL  │
│ React + Vite │   (proxy nginx)     │ Spring Boot  │                │      17      │
│   :80        │                     │    :8080     │                │    :5432     │
└──────────────┘                     └──────────────┘                └──────────────┘
        SPA servida via nginx,            API REST por domínio,         schema 100%
       que faz proxy de /api → :8080      ddl-auto: validate          gerenciado por Flyway
```

A organização do backend é **por domínio** (não por camada técnica), espelhando as áreas da farmácia:

```
br.com.luizgabriel.farmabook
├── order          # encomendas e seus itens  ← núcleo
├── shortage       # faltas + pedidos de reposição
├── compounding    # manipulações  └─ pharmacy  (farmácias de manipulação)
├── prescription   # receitas e seus itens (lote/validade)
├── notification   # notificações WhatsApp (wa.me)
├── customer       # clientes (com telefone)
├── distributor    # distribuidoras
├── catalog        # categorias de produto (enum)
├── auth           # usuários + autenticação por PIN
├── shared         # Auditable (createdAt/updatedAt/deletedAt)
├── exception      # exceções + GlobalExceptionHandler
└── config         # JPA Auditing, BCrypt
```

Cada domínio segue o mesmo formato: `Entity`, `Repository`, `Service` (regra de negócio), `Controller` (REST), `Mapper` (MapStruct) e `dto/` (records de request/response).
 
---

## 🧰 Stack tecnológica

### Backend
| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation) |
| Banco de dados | PostgreSQL 17 |
| Migrações | Flyway (versionadas) |
| Mapeamento DTO↔Entidade | MapStruct |
| Boilerplate | Lombok |
| Hashing de PIN | Spring Security Crypto (BCrypt) |
| Testes | JUnit 5, Mockito, Testcontainers, REST Assured, JSON Unit |

### Frontend
| Camada | Tecnologia |
|---|---|
| Linguagem | TypeScript |
| Framework | React 19 |
| Build | Vite |
| Estilo | TailwindCSS 4 |
| Componentes | Radix UI + lucide-react |
| Estado de servidor | TanStack Query |
| Roteamento | React Router 7 |
| HTTP | Axios |

### Infraestrutura
- **Docker & Docker Compose** — `postgres` + `backend` + `frontend`
- **nginx** — serve a SPA e faz proxy reverso de `/api` (evita CORS)
- **GitHub Actions** — CI roda `mvnw clean verify` a cada push/PR na `main`
> A interface é toda em **português**, com atalhos de teclado pensados para o balcão (ex.: **F2** Faltas, **F3** Encomendas, **F4** Clientes).
 
---

## 💡 Decisões de projeto que valem nota

> Esta seção é sobre o **porquê** de cada escolha — o *como* está em [Os domínios em detalhe](#-os-domínios-em-detalhe).

- **Status derivado, não digitado.** O status da encomenda é sempre o menor status entre os itens, recalculado a cada mudança. Decisão tomada para tornar **impossível** o "caderno inconsistente" que existia no papel — não há como marcar a encomenda como entregue se um item ainda está pendente.
- **Denormalização proposital para histórico.** Nomes de cliente/usuário/distribuidora são copiados para dentro do registro. Renomear um cadastro **não** reescreve o passado — o histórico precisa refletir o que era verdade no momento, não o estado atual.
- **Soft delete em tudo** (via `@SQLDelete` + `@SQLRestriction`). A farmácia precisa de auditoria; "excluir" só marca `deleted_at` e o dado permanece consultável.
- **Schema só via Flyway** (`ddl-auto: validate`). O Hibernate nunca altera o banco; toda mudança é uma migração `V{n}__*.sql` versionada e imutável. Isso dá um schema reproduzível e revisável em PR.
- **Notificação como efeito de transição.** O WhatsApp não é um botão solto — é disparado pela própria transição para "recebido", garantindo que o cliente seja avisado de forma consistente, sem depender de alguém lembrar.
- **Autenticação por PIN, projetada para uso interno.** O FarmaBook foi pensado como ferramenta de balcão numa **rede local da loja** (o mesmo modelo de um PDV de supermercado): PIN curto otimiza a troca de operador no atendimento, e a máquina fica fisicamente atrás do balcão. Por isso não há sessão nem filtro do Spring Security — apenas o header `X-Auth-Pin` resolvido a cada requisição. **Se o sistema fosse exposto à internet pública**, este desenho seria endurecido: troca do PIN por token de sessão após o primeiro login, rate limiting com bloqueio após tentativas falhas, e HTTPS obrigatório. A escolha atual é uma adequação consciente ao contexto de implantação, não uma omissão.
---

## 🧪 Testes e cobertura

O backend tem **459 testes** automatizados, divididos entre **unitários** (serviços, com Mockito) e **de integração** (controllers, com Testcontainers + PostgreSQL real).

| Métrica | Cobertura |
|---|---|
| Classes | **97%** (103/106) |
| Métodos | **97%** (343/353) |
| Linhas | **94%** (1624/1721) | 

```bash
cd backend
 
./mvnw test                                       # todos os testes
./mvnw verify                                     # build completo + testes (igual ao CI)
./mvnw test -Dtest=OrderServiceTest               # uma classe
./mvnw test -Dtest=OrderServiceTest#shouldRecalculateStatusWhenItemChanges   # um método
```

- **Unitários** (`*ServiceTest`): JUnit 5 + Mockito + AssertJ, com fixtures em memória. Cobrem o caminho feliz e cada ramo de falha (não encontrado, conflito, validação), sempre verificando que o efeito colateral **não** aconteceu nos casos de erro.
- **Integração** (`*ControllerTestIT`): um único `PostgreSQLContainer` compartilhado, REST Assured e comparação de JSON com JSON Unit. O estado é limpo a cada teste via `@Sql`.
  A pipeline de **CI** (GitHub Actions) executa `./mvnw clean verify` a cada push e PR na `main`.

---

## 🚀 Começando

### Pré-requisitos
- [Docker](https://www.docker.com/) e Docker Compose
- Para desenvolvimento: **JDK 21** e **Node.js 22+**
### Subir tudo com Docker Compose
A partir da raiz do repositório:

```bash
docker compose up -d --build
```

| Serviço | URL | Descrição |
|---|---|---|
| `frontend` | http://localhost | SPA React servida via nginx |
| `backend` | http://localhost:8080 | API REST Spring Boot |
| `postgres` | localhost:5432 | Banco PostgreSQL 17 |

As credenciais do banco são lidas de um arquivo `.env` na raiz (não versionado). Copie o template e preencha com seus valores:

```bash
cp .envTemplate .env
```

```bash
# .env
ENV_POSTGRES_DB=<nome-do-banco>
ENV_POSTGRES_USER=<usuário>
ENV_POSTGRES_PASSWORD=<senha-forte>
```
 
---

## 🛠 Executando em desenvolvimento

### 1. Banco de dados (obrigatório antes do backend)
```bash
docker compose up -d postgres
```

### 2. Backend
```bash
cd backend
./mvnw spring-boot:run        # aplica as migrações Flyway e sobe a API em :8080
```
Build sem testes:
```bash
./mvnw clean package -DskipTests
```

### 3. Frontend
```bash
cd frontend
npm install
npm run dev                   # Vite com HMR
```
> O cliente HTTP usa `baseURL: '/api'`; em produção o nginx faz o proxy para o backend.

## 🌐 API REST

Todos os endpoints protegidos exigem o header `X-Auth-Pin`. Endpoints de listagem retornam `Page<...>` e aceitam `?page`, `?size` e `?sort=campo,direção`.

### Autenticação — `/auth`
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth/validate-pin` | Valida o PIN de um usuário |
| `POST` | `/auth/change-pin` | Troca o PIN |

### Encomendas — `/orders`
| Método | Rota | Descrição |
|---|---|---|
| `POST` `GET` | `/orders` | Cria / lista (paginado) |
| `GET` `PUT` `DELETE` | `/orders/{id}` | Detalha / atualiza / remove |
| `PATCH` | `/orders/{id}/mark-as-{ordered\|received\|delivered}` | Transição em lote (tolerante) |
| `POST` `PUT` `DELETE` | `/orders/{id}/items[/{itemId}]` | Gerencia itens |
| `PATCH` | `/orders/{id}/items/{itemId}/mark-as-{ordered\|received\|delivered}` | Transição individual (estrita) |
| `PATCH` | `/orders/{id}/items/{itemId}/payment/mark-as-{paid\|to-pay\|make-note\|noted}` | Fluxo de pagamento |

### Demais recursos
| Recurso | Base | Operações |
|---|---|---|
| Usuários | `/users` | CRUD + `activate` / `deactivate` |
| Clientes | `/customers` | CRUD |
| Faltas | `/shortages` | CRUD + `mark-as-ordered` |
| Pedidos de reposição | `/shortage-orders` | CRUD + `mark-as-ordered` |
| Manipulações | `/compoundings` | CRUD + ciclo de vida + pagamento |
| Farmácias de manipulação | `/compounding-pharmacies` | CRUD |
| Receitas | `/prescriptions` | CRUD + itens + `mark-as-received` |
| Distribuidoras | `/distributors` | CRUD |
| Notificações | `/orders/{id}/notifications`, `/notifications/compoundings/{id}`, `/notifications/{id}/resend` | Consulta + reenvio |

Erros são padronizados pelo `GlobalExceptionHandler`: `404` (não encontrado), `409` (conflito de estado), `401` (PIN inválido), `400` (validação).
 
---

## 📁 Estrutura do repositório

```
farmabook/
├── backend/                  # API Spring Boot (módulo Maven)
│   ├── src/main/java/...      # código organizado por domínio
│   ├── src/main/resources/
│   │   └── db/migration/      # migrações Flyway (V1__… em diante)
│   ├── src/test/             # testes unitários (*ServiceTest) e de integração (*ControllerTestIT)
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                 # SPA React + Vite
│   ├── src/
│   │   ├── api/              # clientes HTTP por recurso
│   │   ├── pages/            # páginas por domínio
│   │   ├── components/       # ui (Radix), layout, compartilhados
│   │   ├── context/          # PIN, Toast, Confirm
│   │   └── lib/              # cliente axios + utilitários
│   ├── nginx.conf            # serve a SPA + proxy /api
│   └── Dockerfile
├── docker-compose.yml        # postgres + backend + frontend
├── CLAUDE.md                 # guia de arquitetura e convenções para colaboradores
└── README.md
```

## 👤 Autor

**Luiz Gabriel Costa Britto**

- GitHub: [@luizgabrielcb](https://github.com/luizgabrielcb)
- LinkedIn: [luizgabrielcbritto](https://www.linkedin.com/in/luizgabrielcbritto/)
