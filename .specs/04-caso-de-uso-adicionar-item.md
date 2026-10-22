# 04 - Caso de Uso: Adicionar Item ao Pedido

## Contexto
Após a criação do pedido, o cliente pode necessitar incluir novos itens antes de prosseguir para o pagamento. Na Arquitetura Hexagonal, essa operação deve ser orquestrada por um caso de uso específico na camada de aplicação (`AdicionarItem`). Este caso de uso recupera o agregado existente através do port de saída `Pedidos`, aciona o método de domínio que evolui o pedido de maneira imutável, persiste o agregado atualizado e retorna a nova versão com o total recalculado, expondo a funcionalidade via HTTP no adaptador REST.

---

## Tarefa
Implementar o fluxo completo de adição de item composto por:
1. **Port de Entrada (`AdicionarItem`)**: Interface que declara a operação de adicionar item a um pedido existente (`UUID pedidoId, ItemComando item`).
2. **Exceção de Aplicação (`PedidoNaoEncontradoException`)**: Exceção pura indicando que o ID informado não corresponde a nenhum pedido cadastrado.
3. **Serviço de Aplicação (`AdicionarItemService`)**: Implementação pura do caso de uso.
4. **Configuração de Injeção (`CasosDeUsoConfig`)**: Registro do `@Bean` de `AdicionarItem`.
5. **Endpoint REST no `PedidoController`**: `POST /pedidos/{id}/itens` devolvendo HTTP `200 OK` com o `PedidoResponse` atualizado.
6. **Ampliação do `PedidoExceptionHandler`**: Mapeamento semântico de erros:
   - `PedidoNaoEncontradoException` -> HTTP `404 Not Found`;
   - `IllegalStateException` (pedido fechado/pago/cancelado) -> HTTP `409 Conflict`;
   - `ItemInvalidoException` (quantidade ou preço inválido) -> HTTP `422 Unprocessable Entity`.
7. **Testes Unitários Automatizados**: Cobertura completa com fake em memória para a aplicação e integração ponta a ponta.

---

## Regras de Negócio e Técnicas

### 1. Pureza das Camadas Internas
- `AdicionarItem`, `AdicionarItemService` e `PedidoNaoEncontradoException` pertencem à camada `application/`.
- **PROIBIDO** qualquer import ou anotação de Spring, JPA, HTTP ou adaptadores nessas classes.
- O caso de uso `CriarPedido` e o núcleo de `domain/` permanecem intocados.

### 2. Busca e Validação de Existência
- O caso de uso consulta o pedido pelo port `Pedidos.porId(pedidoId)`.
- Se o pedido não existir, deve lançar obrigatoriamente `PedidoNaoEncontradoException`, que o handler REST converte para HTTP `404 Not Found`.

### 3. Integridade e Regras de Domínio
- O serviço chama o método de comportamento do domínio `pedido.adicionarItem(novoItem)`.
- **Pedido Fechado:** Se o pedido estiver `PAGO` ou `CANCELADO`, o domínio recusa a adição lançando `IllegalStateException`, que o handler REST converte para HTTP `409 Conflict`.
- **Item Inválido:** Se a quantidade for `<= 0` ou o preço unitário for `<= 0`, o domínio recusa lançando `ItemInvalidoException`, que o handler REST converte para HTTP `422 Unprocessable Entity`.

### 4. Recálculo do Total e Persistência
- A adição do item devolve uma nova instância de `Pedido` com o mesmo `id`, mesmo `clienteId`, mesmo `status` e a lista acumulada de itens.
- O valor `total()` derivado é recalculado automaticamente no domínio.
- O agregado atualizado é salvo através do port `pedidos.salvar(pedidoAtualizado)`.

### 5. Atomicidade
- Em qualquer cenário de recusa (404, 409, 422 ou 400), nenhuma linha deve ser gravada na tabela `item_pedido`.

### 6. Contrato HTTP
- **Rota:** `POST /pedidos/{id}/itens`
- **Payload de Requisição (`ItemRequest`):**
  ```json
  {
    "produtoId": "CAFE-500",
    "quantidade": 1,
    "precoUnitario": 18.90
  }
  ```
- **Resposta (HTTP 200 OK):**
  ```json
  {
    "id": "<mesmo-uuid>",
    "clienteId": "c-1",
    "status": "ABERTO",
    "itens": [
      {
        "produtoId": "CAFE-500",
        "quantidade": 2,
        "precoUnitario": 18.90
      },
      {
        "produtoId": "CAFE-500",
        "quantidade": 1,
        "precoUnitario": 18.90
      }
    ],
    "total": 56.70
  }
  ```

---

## Definição de Pronto (Definition of Done) - Casos de Teste

### Casos de Teste Unitário (`AdicionarItemServiceTest`)
1. **Adição bem-sucedida:** Adiciona item válido a pedido aberto existente; valida que o retorno possui o mesmo UUID, itens acumulados e total atualizado.
2. **Pedido inexistente:** Tentar adicionar item a UUID inexistente deve lançar `PedidoNaoEncontradoException`.
3. **Pedido fechado:** Tentar adicionar item a pedido com status `PAGO` ou `CANCELADO` deve propagar `IllegalStateException`.
4. **Item com quantidade inválida:** Tentar adicionar item com quantidade `<= 0` deve propagar `ItemInvalidoException`.
5. **Item com preço inválido:** Tentar adicionar item com preço `<= 0` deve propagar `ItemInvalidoException`.

### Casos de Teste HTTP Verificáveis (Ponta a Ponta)
1. **Cenário Canônico (200):**
   - Pedido existente de `c-1` com 2 x 18.90 (total 37.80) recebe `POST /pedidos/{id}/itens` com 1 unidade a 18.90.
   - Retorno: HTTP `200 OK`, mesmo UUID, lista com 2 itens e total derivado igual a `56.70`.
2. **Cenário Pedido Inexistente (404):**
   - Enviar `POST /pedidos/<uuid-aleatorio>/itens` com item válido.
   - Retorno: HTTP `404 Not Found` com mensagem descritiva.
   - Verificação no banco: nenhuma linha inserida.
3. **Cenário Pedido Fechado (409):**
   - Tentar adicionar item a pedido cujo status não seja `ABERTO`.
   - Retorno: HTTP `409 Conflict` com mensagem descritiva.
   - Verificação no banco: nenhuma linha inserida.
4. **Cenário Quantidade Inválida (422):**
   - Enviar item com `quantidade: 0`.
   - Retorno: HTTP `422 Unprocessable Entity` com mensagem descritiva.
   - Verificação no banco: nenhuma linha inserida.

---

## Limites de Arquivos (File Boundaries)

### Arquivos a Serem Criados:
- `src/main/java/br/com/pedidos/api/application/port/in/AdicionarItem.java`
- `src/main/java/br/com/pedidos/api/application/service/AdicionarItemService.java`
- `src/main/java/br/com/pedidos/api/application/exception/PedidoNaoEncontradoException.java`
- `src/test/java/br/com/pedidos/api/application/service/AdicionarItemServiceTest.java`

### Arquivos a Serem Modificados:
- `src/main/java/br/com/pedidos/api/config/CasosDeUsoConfig.java` (adicionar `@Bean AdicionarItem`)
- `src/main/java/br/com/pedidos/api/adapters/entrada/rest/PedidoController.java` (adicionar método `POST /pedidos/{id}/itens`)
- `src/main/java/br/com/pedidos/api/adapters/entrada/rest/PedidoExceptionHandler.java` (adicionar handlers para 404 e 409)

### Arquivos Intocados:
- `br.com.pedidos.api.domain.*`, exceto: criação de `PedidoFechadoException` (extends `IllegalStateException`) e seu uso em `Pedido.adicionarItem/pagar/cancelar`. O handler REST mapeia para 409 somente `PedidoFechadoException`.
- `br.com.pedidos.api.application.port.in.CriarPedido` e `CriarPedidoService` (permanecem intactos)
- Nenhuma dependência nova no `pom.xml`
- Nenhum commit no Git
