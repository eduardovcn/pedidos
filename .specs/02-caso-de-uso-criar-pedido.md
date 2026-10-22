# 02 - Caso de Uso: Criar Pedido

## Contexto
Com o núcleo de domínio (`Pedido`, `ItemPedido`, `StatusPedido`) implementado e testado de maneira pura, a camada de aplicação deve orquestrar as operações de negócio. O caso de uso de criação de pedidos recebe os dados do cliente e os itens solicitados, valida a integridade da operação no nível de aplicação, coordena as regras de domínio e persiste o agregado através de uma porta de saída (port out), mantendo total isolamento de frameworks externos e persistência.

---

## Tarefa
Implementar o fluxo de aplicação para a criação de pedidos composto por:
1. **Port de Entrada (`CriarPedido`)**: Interface que define a operação de criar pedido, recebendo os dados necessários (cliente e itens).
2. **Serviço de Aplicação (`CriarPedidoService`)**: Implementação do caso de uso que valida a lista de itens, orquestra o domínio e aciona a persistência.
3. **Port de Saída (`Pedidos`)**: Interface que abstrai o armazenamento e persistência de pedidos (`salvar(Pedido pedido)`).
4. **Testes Unitários da Aplicação**: Suíte pura em JUnit 5 utilizando uma implementação de `Pedidos` em memória (Fake) dentro do pacote de teste.

---

## Regras de Negócio e Técnicas

### 1. Pureza da Camada de Aplicação
- A camada de aplicação deve ser 100% Java puro.
- **PROIBIDO** qualquer import ou anotação de Spring Framework (`@Service`, `@Component`, etc.), JPA, HTTP ou classes de adaptadores de infraestrutura.
- A comunicação com o exterior deve ocorrer estritamente através das portas (`CriarPedido` na entrada e `Pedidos` na saída).

### 2. Validação da Requisição de Criação
- O caso de uso recebe o identificador do cliente e a lista de itens desejados.
- **Lista de Itens Obrigatória e Não Vazia:** Se a lista de itens for nula ou vazia, o caso de uso deve recusar a criação lançando `IllegalArgumentException`.
- **Identificação do Cliente:** O identificador do cliente não pode ser nulo nem em branco (`IllegalArgumentException`).

### 3. Orquestração com o Domínio
- O serviço deve instanciar o agregado inicial através de `Pedido.novo(clienteId)`.
- Cada item deve ser incorporado ao pedido respeitando as regras do domínio (imutabilidade via `adicionarItem`).

### 4. Persistência e Retorno
- Após construir o pedido completo, o serviço deve delegar a persistência à interface de saída `Pedidos` chamando o método de salvar.
- O caso de uso deve retornar a instância de `Pedido` resultante da persistência.

---

## Definição de Pronto (Definition of Done) - Casos de Teste

A implementação será considerada concluída quando os seguintes cenários passarem com 100% de sucesso em testes unitários puros:

1. **Criação bem-sucedida de pedido com itens:**
   - Ao executar `CriarPedido` informando cliente `c-1` e ao menos um item válido (ex: `CAFE-500`, 2 unidades, 18.90), o pedido deve ser criado com status `ABERTO`, persistido na porta de saída `Pedidos` e devolvido com total `37.80`.
2. **Persistência confirmada na porta de saída:**
   - O pedido retornado deve estar presente na implementação em memória de `Pedidos`, podendo ser consultado por seu `id`.
3. **Recusa de criação com lista de itens vazia:**
   - Tentar criar pedido passando lista de itens vazia deve lançar `IllegalArgumentException`.
4. **Recusa de criação com lista de itens nula:**
   - Tentar criar pedido passando lista de itens nula deve lançar `IllegalArgumentException` ou `NullPointerException`.
5. **Recusa de criação com cliente nulo ou em branco:**
   - Tentar criar pedido sem informar o cliente deve lançar exceção de validação.
6. **Propagação de regras do domínio:**
   - Itens com quantidade ou preço unitário inválidos continuam sendo barrados pelas regras do domínio.
