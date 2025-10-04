# Distributed Chat - Spring Boot

Sistema distribuído de chat cliente-servidor desenvolvido em **Java** com **Spring Boot**.

Permite a criação de grupos, envio e leitura de mensagens via API REST (HTTP/1.1 + JSON), com suporte a múltiplos clientes simultâneos, deduplicação de mensagens e registro de logs. Projeto acadêmico para disciplina de Sistemas Distribuídos.

---

## Funcionalidades

- Criar e listar grupos.
- Postar mensagens em grupos.
- Ler mensagens a partir de um cursor temporal.
- Postagem anônima com nickname.
- Idempotência para evitar duplicação de mensagens.
- Controle de número de conexões simultâneas.
- Logs de requisições, erros e latência.

---

## Tecnologias

- Java 24
- Spring Boot
- Maven
- Banco de dados (PostgreSQL)
- JSON + HTTP/1.1

---

## Endpoints principais

- `POST /nick` → Registrar nickname
- `POST /groups` → Criar grupo
- `GET /groups` → Listar grupos
- `POST /groups/{id}/messages` → Postar mensagem (`idemKey`, `text`, `timestamp_client`)
- `GET /groups/{id}/messages?since=[cursor]&limit=10` → Ler mensagens

---