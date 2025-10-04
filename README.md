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

- Java 24 (compatível com Java 21+)
- Spring Boot
- Maven
- Banco de dados (MySQL em produção; H2 para testes)
- JSON + HTTP/1.1
- JavaFX para cliente desktop

---

## Endpoints principais

- `POST /nick` → Registrar nickname
- `POST /groups` → Criar grupo
- `GET /groups` → Listar grupos
- `POST /groups/{id}/messages` → Postar mensagem (`idemKey`, `text`, `timestamp_client`)
- `GET /groups/{id}/messages?since=[cursor]&limit=10` → Ler mensagens

---

## Como executar o servidor

1. Configure o banco (application.properties) para seu MySQL ou use o perfil de testes (H2) conforme `src/main/resources/application-test.properties`.
2. Execute a aplicação Spring Boot (classe `br.distributed.system.chat.ChatApplication`).

---

## Cliente JavaFX (GUI)

Foi adicionada uma interface JavaFX mínima para criação de grupos e abertura de múltiplas janelas de clientes, cada uma com seu próprio nick. Em cada janela é possível enviar e ler mensagens do grupo com atualização automática.

- Classe principal: `br.distributed.system.chat.fx.AppLauncher`
- Como executar pela linha de comando (com o JavaFX Maven Plugin):

```
mvn -DskipTests javafx:run -Djavafx.mainClass=br.distributed.system.chat.fx.AppLauncher
```

- Como executar pela IDE: rode o método `main` de `AppLauncher`.

### Fluxo de uso

1. Abra o cliente JavaFX.
2. Informe a URL do servidor (ex.: `http://localhost:8080`).
3. Crie um grupo informando o nome e clique em "Criar grupo". Use "Atualizar" para ver a lista.
4. Informe seu nick e o ID do grupo e clique em "Abrir cliente" para abrir uma nova janela.
5. Na janela do cliente, digite mensagens e clique em "Enviar". As mensagens do grupo serão atualizadas automaticamente.

Observações:
- O servidor limita a 5 clientes simultâneos. Ao abrir o 6º, um cliente ocioso (> 15s sem envio) ou o mais antigo é desconectado. O cliente precisa re-registrar o nick ao enviar novamente (tratado automaticamente pelo cliente de referência). 
- A leitura usa cursor temporal baseado em `timestamp_server` para garantir ordenação e paginação incremental.

---