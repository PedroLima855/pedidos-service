# 📦 Pedidos Service

API REST para gerenciamento de pedidos com sistema de créditos para parceiros, construída com **Spring Boot 4** e **PostgreSQL**.

---

## 🚀 Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.6 |
| Spring Data JPA | 4.0.5 |
| PostgreSQL | 15+ |
| Flyway | 11.14 |
| Lombok | 1.18 |
| SpringDoc OpenAPI | 3.0.2 |

---

## 📐 Arquitetura

```
src/main/java/com/pedido/
├── controller/       → Endpoints REST
├── dto/              → Objetos de transferência (request/response)
├── entity/           → Entidades JPA
├── enums/            → Enumerações (StatusPedido)
├── event/            → Eventos de domínio e listeners
├── exception/        → Tratamento global de exceções
├── repository/       → Repositórios Spring Data
└── service/          → Regras de negócio
```

---

## 🗄️ Modelo de Dados

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   parceiro   │       │    pedido    │       │  item_pedido │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id           │◄──────│ parceiro_id  │       │ id           │
│ nome         │       │ id           │◄──────│ pedido_id    │
│ limite_cred. │       │ valor_total  │       │ produto      │
│ cred._disp.  │       │ status       │       │ quantidade   │
│ criado_em    │       │ criado_em    │       │ preco_unit.  │
└──────────────┘       │ atualizado_em│       └──────────────┘
                       └──────────────┘
```

---

## 📋 Funcionalidades

### ✅ Gerenciamento de Pedidos
- Cadastro de pedidos com lista de itens
- Consulta por ID, por status e por período
- Atualização de status com validação de transições
- Cancelamento com estorno automático de crédito

### 💳 Sistema de Créditos
- Cada parceiro possui um limite de crédito
- Ao criar um pedido, valida se o parceiro tem crédito suficiente
- Ao aprovar, debita o valor do crédito disponível
- Ao cancelar um pedido aprovado, estorna o crédito automaticamente

### 🔔 Notificações
- Eventos de domínio disparados a cada mudança de status
- Listener assíncrono simulando integração com mensageria (SQS, Kafka, etc.)
- Logs detalhados de cada notificação enviada

### 🔄 Fluxo de Status

```
PENDENTE → APROVADO → EM_PROCESSAMENTO → ENVIADO → ENTREGUE
    │          │              │               │
    └──────────┴──────────────┴───────────────┴──→ CANCELADO
```

> Pedidos **CANCELADOS** ou **ENTREGUES** não podem ter status alterado.

---

## ⚙️ Configuração

### Pré-requisitos
- Java 17+
- PostgreSQL rodando em `localhost:5432`
- Banco de dados `pdd_dados` criado

### Criar o banco
```sql
CREATE DATABASE pdd_dados;
```

### Configuração (`application.properties`)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pdd_dados
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### Executar (local)
```bash
./mvnw spring-boot:run
```

### Executar (Docker)
```bash
docker compose up --build
```

> O Flyway cria as tabelas automaticamente na primeira execução.

---

## 🌐 Endpoints

### Parceiros

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/parceiros` | Criar parceiro |
| `GET` | `/api/parceiros` | Listar todos |
| `GET` | `/api/parceiros/{id}` | Buscar por ID |

### Pedidos

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/pedidos` | Criar pedido |
| `GET` | `/api/pedidos/{id}` | Buscar por ID |
| `GET` | `/api/pedidos/status/{status}` | Buscar por status |
| `GET` | `/api/pedidos/periodo?inicio=...&fim=...` | Buscar por período |
| `PATCH` | `/api/pedidos/{id}/status` | Atualizar status |
| `POST` | `/api/pedidos/{id}/cancelar` | Cancelar pedido |

---

## 📝 Exemplos de Uso

### Criar parceiro
```bash
curl -X POST http://localhost:8080/api/parceiros \
  -H "Content-Type: application/json" \
  -d '{"nome": "Parceiro Alpha", "limiteCredito": 10000.00}'
```

### Criar pedido
```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "parceiroId": 1,
    "itens": [
      {"produto": "Notebook Dell", "quantidade": 2, "precoUnitario": 3500.00},
      {"produto": "Mouse Logitech", "quantidade": 5, "precoUnitario": 150.00}
    ]
  }'
```

### Aprovar pedido
```bash
curl -X PATCH http://localhost:8080/api/pedidos/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "APROVADO"}'
```

### Cancelar pedido
```bash
curl -X POST http://localhost:8080/api/pedidos/1/cancelar
```

### Buscar por período
```bash
curl "http://localhost:8080/api/pedidos/periodo?inicio=2026-01-01T00:00:00&fim=2026-12-31T23:59:59"
```

---

## 📖 Documentação Interativa

Após iniciar a aplicação, acesse o Swagger UI:

🔗 **http://localhost:8080/swagger-ui.html**

---

## 🛡️ Tratamento de Erros

A API retorna respostas padronizadas para erros:

| Status | Cenário |
|--------|---------|
| `400` | Validação de campos (campos obrigatórios, valores inválidos) |
| `404` | Recurso não encontrado (pedido ou parceiro inexistente) |
| `422` | Regra de negócio violada (crédito insuficiente, transição inválida) |

Exemplo de resposta de erro:
```json
{
  "erro": "Crédito insuficiente. Disponível: 5000.00, Necessário: 80000.00",
  "timestamp": "2026-06-06T20:30:00"
}
```

---

## 🗂️ Migrations (Flyway)

| Versão | Arquivo | Descrição |
|--------|---------|-----------|
| V1 | `V1__criar_tabelas.sql` | Criação das tabelas `parceiro`, `pedido` e `item_pedido` |

As migrations ficam em `src/main/resources/db/migration/` e são executadas automaticamente ao subir a aplicação.

---

## 📬 Sistema de Notificações

O serviço utiliza **Spring Application Events** com processamento assíncrono para simular integração com mensageria:

```
[Mudança de Status] → [PedidoStatusAlteradoEvent] → [NotificacaoListener] → [Log/Mensageria]
```

Exemplo de log gerado:
```
[NOTIFICAÇÃO] Pedido #1 teve status alterado: PENDENTE -> APROVADO
[MENSAGERIA] Mensagem enviada para fila de notificações - Pedido #1
```

---

## 🧪 Testes Automatizados

O projeto possui cobertura completa com **36 testes** entre unitários e de integração.

### Testes Unitários
- `ParceiroServiceTest` — criação, busca e listagem de parceiros
- `PedidoServiceTest` — criação de pedidos, validação de crédito, transições de status, cancelamento e estorno

### Testes de Integração
- `ParceiroControllerIntegrationTest` — endpoints REST de parceiros com validações
- `PedidoControllerIntegrationTest` — fluxo completo de pedidos (criação, aprovação, cancelamento, estorno de crédito)

### Executar
```bash
./mvnw test
```

> Os testes de integração usam banco H2 em memória, não precisam de PostgreSQL rodando.

---

## 🐳 Docker Compose

O projeto inclui `docker-compose.yml` para subir toda a aplicação com um único comando.

### Pré-requisito
- Docker e Docker Compose instalados

### Executar
```bash
docker compose up --build
```

Isso irá:
1. Subir o **PostgreSQL 15** com o banco `pdd_dados` criado automaticamente
2. Fazer o **build** da aplicação Java
3. Subir a **API** na porta `8080` (aguarda o banco estar pronto via healthcheck)
4. O **Flyway** cria as tabelas automaticamente na primeira execução

### Parar
```bash
docker compose down -v
```

---

## 📄 Licença

Este projeto é de uso livre para fins de estudo e desenvolvimento.
