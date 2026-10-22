# 01 - Domínio Inicial de Pedidos

## Contexto
O serviço `api` gerencia o ciclo de vida e processamento de pedidos de compras. Para garantir integridade e aderência aos princípios da Arquitetura Hexagonal, o núcleo do negócio deve ser completamente isolado de frameworks e detalhes de persistência. Este núcleo precisa modelar o comportamento de um pedido de forma imutável, garantindo que regras como cálculo de totais, transições de estado e integridade de itens sejam rigorosamente preservadas desde a criação.

---

## Tarefa
Modelar o domínio inicial de pedidos composto por:
- **`Pedido`**: Agregado raiz que gerencia o ciclo de vida, seus itens e o cálculo financeiro derivado.
- **`ItemPedido`**: Item associado a um pedido, contendo identificação do produto, quantidade e preço unitário.
- **`StatusPedido`**: Enumeração dos estados possíveis do pedido (`ABERTO`, `PAGO`, `CANCELADO`).

A implementação deve priorizar **linguagem de comportamento** em vez de métodos puramente estruturais, adotando **Records** do Java 21 e técnicas de **imutabilidade com cópia defensiva**.

---

## Regras de Negócio e Técnicas

### 1. Pureza do Domínio
- O domínio deve ser composto exclusivamente por tipos nativos do Java 21 (Records, Enums, Interfaces).
- **PROIBIDO** qualquer import ou anotação de Spring Framework (`@Component`, etc.) ou JPA/Hibernate (`@Entity`, `@Table`, etc.).

### 2. Estado Inicial e Criação
- Um novo pedido é criado através do método de comportamento de fábrica `Pedido.novo(...)`.
- Um pedido recém-criado nasce como um rascunho com status **`ABERTO`**, lista de itens vazia e identificador único (**`UUID`**) gerado.
- Exemplo do fluxo: cliente `c-1` inicia um pedido vazio.

### 3. Integridade de Itens e Valores
- **Quantidade:** Deve ser estritamente positiva (inteiro maior que zero: `quantidade > 0`).
- **Preço Unitário:** Todo valor monetário deve ser representado com `BigDecimal` e deve ser estritamente positivo (`precoUnitario.compareTo(BigDecimal.ZERO) > 0`).
- **Subtotal do Item:** Calculado como `quantidade * precoUnitario`.
- Exemplo canônico: cliente `c-1` adiciona o produto `CAFE-500`: `2 x 18.90 = 37.80`.

### 4. Cálculo do Total do Pedido
- O valor total do pedido é um **valor derivado**: calculado dinamicamente a partir da soma dos subtotais dos itens.
- Um pedido sem itens tem valor total igual a `BigDecimal.ZERO`.

### 5. Imutabilidade e Evolução por Cópia Defensiva
- A lista de itens exposta por `Pedido` deve ser imutável (ex.: `Collections.unmodifiableList` ou `List.copyOf`), impedindo qualquer modificação externa direta.
- A operação `adicionarItem` não modifica o objeto existente; ela **retorna uma nova instância de `Pedido`** contendo os itens anteriores mais o novo item (mantendo o mesmo `id`, `clienteId` e `status`).

### 6. Transições de Status e Restrições de Operação
- Um pedido com status **`ABERTO`** pode:
  - Adicionar itens (`adicionarItem`);
  - Transitar para **`PAGO`** (`pagar`);
  - Transitar para **`CANCELADO`** (`cancelar`).
- Um pedido com status **`PAGO`** ou **`CANCELADO`**:
  - **NÃO aceita** adição de novos itens (a operação é recusada com erro);
  - **NÃO permite** nova transição de status (transições a partir de `PAGO` ou `CANCELADO` são recusadas com erro).

---

## Definição de Pronto (Definition of Done) - Casos de Teste

A implementação será considerada concluída quando uma suite de testes unitários pura (sem dependência de contexto Spring) validar com 100% de sucesso os seguintes casos:

### Casos de Sucesso
1. **Criação de pedido novo:**
   - Ao chamar `Pedido.novo("c-1")`, deve retornar um `Pedido` com `id` (UUID não nulo), `clienteId = "c-1"`, status `ABERTO`, lista de itens vazia e total `BigDecimal.ZERO`.
2. **Adição de item em pedido aberto com novo objeto retornado:**
   - Ao adicionar item (`produtoId = "CAFE-500"`, `quantidade = 2`, `precoUnitario = 18.90`) a um pedido aberto, a instância original permanece vazia e uma **nova instância** é retornada contendo 1 item.
3. **Cálculo de subtotal do item e total derivado do pedido:**
   - Para o item `CAFE-500` com 2 unidades a R$ 18.90, o subtotal do item deve ser `37.80`.
   - O total do pedido deve refletir a soma exata dos subtotais (ex.: `37.80`).
4. **Adição de múltiplos itens:**
   - Adicionar sucessivos itens deve acumular corretamente na nova instância e refletir no total derivado.
5. **Transição de ABERTO para PAGO:**
   - Ao acionar o comportamento `pagar()` em um pedido `ABERTO`, deve retornar uma nova instância com status `PAGO`.
6. **Transição de ABERTO para CANCELADO:**
   - Ao acionar o comportamento `cancelar()` em um pedido `ABERTO`, deve retornar uma nova instância com status `CANCELADO`.
7. **Imutabilidade da coleção externa:**
   - Tentar executar operações de escrita (como `.add()` ou `.clear()`) na lista retornada por `pedido.itens()` deve lançar `UnsupportedOperationException`.

### Casos de Erro e Validação
8. **Item com quantidade inválida:**
   - Tentar criar `ItemPedido` com quantidade zero ou negativa (`<= 0`) deve lançar exceção de validação de domínio.
9. **Item com preço unitário inválido:**
   - Tentar criar `ItemPedido` com preço nulo, zero ou negativo (`<= 0`) deve lançar exceção de validação de domínio.
10. **Recusa de adição de item em pedido PAGO:**
    - Invocar `adicionarItem` em um pedido que já está com status `PAGO` deve lançar exceção de estado inválido.
11. **Recusa de adição de item em pedido CANCELADO:**
    - Invocar `adicionarItem` em um pedido que já está com status `CANCELADO` deve lançar exceção de estado inválido.
12. **Transições inválidas de status:**
    - Tentar pagar um pedido já `PAGO` ou já `CANCELADO` deve lançar exceção de transição inválida.
    - Tentar cancelar um pedido já `PAGO` ou já `CANCELADO` deve lançar exceção de transição inválida.
