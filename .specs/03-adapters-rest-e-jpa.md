# 03 - Adaptadores de Infraestrutura: REST e JPA

## Contexto
Com o núcleo de domínio (`Pedido`, `ItemPedido`, `StatusPedido`) e a camada de aplicação (`CriarPedido`, `CriarPedidoService`, `Pedidos`) desenvolvidos e testados de forma pura, a etapa seguinte da Arquitetura Hexagonal é a implementação dos **Adaptadores de Infraestrutura**. Esses adaptadores conectam a aplicação ao mundo externo:
1. **Adaptador de Persistência (Driven / Outbound):** Implementação JPA com Spring Data para persistir e recuperar o agregado `Pedido` no banco de dados relacional PostgreSQL.
2. **Adaptador Web/REST (Driving / Inbound):** Controllers HTTP que expõem endpoints para clientes externos, validando payloads e acionando os casos de uso.

Para garantir robustez e rastreabilidade, esta especificação estabelece **Três Provas de Verificação**:
- **Prova 1:** Banco de dados PostgreSQL disponível e saudável via Docker Compose.
- **Prova 2:** Persistência íntegra do agregado através do adapter JPA (`PedidosJpaAdapter`), com mapeamento manual, transacionalidade estrita e sem vazar ORM para o domínio.
- **Prova 3:** Entrega da interface HTTP/REST recebendo requisições, orquestrando a criação do pedido, tratando erros com códigos HTTP semânticos (400, 422, 201) e garantindo a sobrevivência dos dados após reinicialização.

---

## Tarefa
Implementar os adaptadores de infraestrutura em fases (checkpoints):

### Checkpoint 4B - Adaptador de Persistência JPA (Postgres) [CONCLUÍDO]
1. **Entidades JPA (`infrastructure.persistence.entity`):**
   - `PedidoJpaEntity`: mapeia a tabela `pedido`, utilizando o `UUID` do domínio como chave primária (`@Id`), sem geração automática de ID e sem coluna `total` (o total é derivado dos itens).
   - `ItemJpaEntity`: mapeia a tabela `item_pedido`, associada ao pedido, guardando `sku` (correspondente ao `produtoId`), `quantidade` e `precoUnitario`.
2. **Repositório Spring Data (`infrastructure.persistence.repository`):**
   - `SpringDataPedidoRepository`: interface estendendo `JpaRepository<PedidoJpaEntity, UUID>`.
3. **Mapper Manual (`infrastructure.persistence.mapper`):**
   - `PedidoJpaMapper`: conversão bidirecional entre o agregado de domínio `Pedido` e as entidades `PedidoJpaEntity` / `ItemJpaEntity`, sem bibliotecas externas (sem MapStruct/Lombok).
4. **Adapter de Persistência (`infrastructure.persistence`):**
   - `PedidosJpaAdapter`: implementa o port de saída `Pedidos`, anotado com `@Repository` / `@Transactional`, garantindo a leitura e hidratação completa dos itens antes do fechamento da sessão.
5. **Configuração da Aplicação (`application.yml`):**
   - Configurar DataSource para o PostgreSQL local (`localhost:5433/pedidos`), dialeto PostgreSQL, `spring.jpa.open-in-view: false` e DDL apropriado.
6. **Testes de Integração Explícitos (`*IT`):**
   - Criação de `PedidosJpaAdapterIT` e adaptação do teste gerado de contexto para `ApiApplicationIT`.
   - Execução explícita contra o PostgreSQL local.

### Checkpoint 4C - Adaptador Web REST (HTTP)
1. **Contrato de Requisição e Resposta (`adapters.entrada.rest.dto`):**
   - `PedidoRequest` e `ItemRequest`: DTOs de entrada com anotações Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`) para presença e formato.
   - `PedidoResponse` e `ItemResponse`: DTOs de saída com `id` (UUID), `clienteId`, `status`, `itens` e `total` como números decimais (`BigDecimal`).
2. **Controller REST (`adapters.entrada.rest.PedidoController`):**
   - Mapeia `POST /pedidos`.
   - Injeta exclusivamente o port de entrada `CriarPedido` (não conhece JPA nem persistência direta).
   - Retorna HTTP `201 Created` com cabeçalho `Location: /pedidos/{id}` e o corpo `PedidoResponse`.
3. **Tratamento Global de Exceções (`adapters.entrada.rest.PedidoExceptionHandler`):**
   - Mapeia `ItemInvalidoException` (ou validação de item) -> HTTP `422 Unprocessable Entity` com mensagem explicativa.
   - Mapeia `PedidoSemItensException` (ou tentativa de pedido vazio) -> HTTP `422 Unprocessable Entity` com mensagem explicativa.
   - Mapeia JSON malformado (`HttpMessageNotReadableException`) ou campos obrigatórios ausentes (`MethodArgumentNotValidException`) -> HTTP `400 Bad Request`.
4. **Configuração de Casos de Uso (`config.CasosDeUsoConfig`):**
   - Classe de configuração do Spring (`@Configuration`) que instancia `CriarPedidoService` como `@Bean`, recebendo o port `Pedidos` e preservando a pureza da aplicação.

---

## Regras de Negócio e Técnicas

### 1. Pureza das Camadas Internas
- O domínio (`br.com.pedidos.api.domain`) e a aplicação (`br.com.pedidos.api.application`) **permanecem agnósticos de framework**.
- Exceções específicas do domínio/aplicação (`ItemInvalidoException`, `PedidoSemItensException`) devem herdar de tipos padrão (`IllegalArgumentException` ou `RuntimeException`), mantendo total compatibilidade com os testes existentes.

### 2. Contrato HTTP do Endpoint `POST /pedidos`
- **Request Payload:**
  ```json
  {
    "clienteId": "c-1",
    "itens": [
      {
        "produtoId": "CAFE-500",
        "quantidade": 2,
        "precoUnitario": 18.90
      }
    ]
  }
  ```
- **Response Payload (HTTP 201 Created):**
  - Header: `Location: /pedidos/<uuid>`
  - Body:
    ```json
    {
      "id": "<uuid>",
      "clienteId": "c-1",
      "status": "ABERTO",
      "itens": [
        {
          "produtoId": "CAFE-500",
          "quantidade": 2,
          "precoUnitario": 18.90
        }
      ],
      "total": 37.80
    }
    ```

### 3. Validação e Semântica de Erros HTTP
- **HTTP 400 (Bad Request):** JSON sintaticamente malformado ou campos obrigatórios do payload ausentes/nulos (`clienteId`, `itens`).
- **HTTP 422 (Unprocessable Entity):** Violação de regras semânticas de negócio:
  - Quantidade zero ou negativa (`quantidade <= 0`);
  - Preço unitário zero ou negativo (`precoUnitario <= 0`);
  - Lista de itens vazia.
- **Atomicidade:** Em qualquer cenário de erro/recusa (400 ou 422), **nenhuma linha** deve ser inserida nas tabelas `pedido` ou `item_pedido`.

---

## Definição de Pronto (Definition of Done) - As Três Provas

### Prova 1: Banco de Dados Disponível [VERIFICADA]
- PostgreSQL 16 operacional no Docker (`pg_isready` e `SELECT 1;` validados).

### Prova 2: Persistência pelo Adapter JPA [VERIFICADA]
- `PedidosJpaAdapterIT` executado com sucesso contra o Postgres, persistindo e recuperando o pedido em transação dedicada.

### Prova 3: Interface HTTP / REST (Checkpoint 4C)
1. **Execução Prévia de Testes:**
   - Testes unitários puros (`*Test`) passando 100% sem banco.
   - Testes de integração (`*IT`) passando 100% com o Postgres.
2. **Execução Real contra a Aplicação em Execução:**
   - **Caso Sucesso (201):** `POST /pedidos` com `CAFE-500` (2 x 18.90) -> retorna HTTP 201, header `Location` e `total: 37.80`.
   - **Caso Quantidade Zero (422):** `POST /pedidos` com `quantidade: 0` -> retorna HTTP 422 e nenhuma linha inserida no banco.
   - **Caso Lista Vazia (422):** `POST /pedidos` com `itens: []` -> retorna HTTP 422 e nenhuma linha inserida no banco.
   - **Caso JSON Malformado (400):** `POST /pedidos` com JSON inválido -> retorna HTTP 400 e nenhuma linha inserida no banco.
3. **Persistência e Reinicialização:**
   - Consulta via `SELECT` ao pedido criado comprovando presença no banco.
   - Reinicialização da aplicação e nova verificação comprovando que os dados persistem íntegros.
