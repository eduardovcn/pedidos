# 05 - Pagamentos Assíncronos Simulados

## Contexto
Hoje o pedido só tem `ABERTO`, `PAGO` e `CANCELADO`, e a aprovação é uma transição síncrona do domínio (`Pedido.pagar()`). Esta spec move o pagamento para um fluxo assíncrono **simulado** via RabbitMQ (já no `infra/docker-compose.yml`). Não há cobrança real.

## Tarefa
1. `POST /pedidos/{pedidoId}/pagamentos` publica uma solicitação de pagamento e responde `202`.
2. Um processador simulado consome `pagamentos.solicitacoes` e publica o resultado.
3. O serviço de pedidos consome `pedidos.resultados` e conclui o pedido.
4. Remove-se a aprovação síncrona antiga (`Pedido.pagar()` e seus usos).

## Regras

### 1. Topologia RabbitMQ (declarada por inteiro desde o início)
Todas as filas são `durable` e as mensagens `persistent`. Os argumentos das filas ficam definitivos já na primeira declaração, sem mudanças depois.

| Recurso | Tipo | Detalhe |
|---|---|---|
| `pagamentos.comandos` | exchange direct | chave `pagamento.solicitar` → `pagamentos.solicitacoes` |
| `pagamentos.eventos` | exchange direct | chave `pagamento.resultado` → `pedidos.resultados` |
| `pagamentos.solicitacoes` | fila | `x-dead-letter-exchange=""`, `x-dead-letter-routing-key=pagamentos.solicitacoes.erros` |
| `pedidos.resultados` | fila | `x-dead-letter-exchange=""`, `x-dead-letter-routing-key=pedidos.resultados.erros` |
| `pagamentos.solicitacoes.erros` | fila (DLQ) | sem argumentos de DLX |
| `pedidos.resultados.erros` | fila (DLQ) | sem argumentos de DLX |

- O dead-letter usa o exchange padrão (`""`) com a chave igual ao nome da DLQ, sem exchanges extras.
- Falha de processamento = mensagem rejeitada sem reenfileirar (`requeue=false`) → DLQ.
- Retentativas do listener são limitadas (ex.: 3) antes de ir para a DLQ. Não pode haver loop infinito.

### 2. Contrato HTTP
`POST /pedidos/{pedidoId}/pagamentos`

```json
{ "solicitacaoId": "6f1c...-uuid", "cenario": "APROVAR" }
```
- `cenario`: `APROVAR` ou `RECUSAR`.
- O **valor vem do pedido** (`Pedido.total()`). O cliente não envia valor, e qualquer campo extra é ignorado.
- `POST /pedidos` e `GET /pedidos/{id}` são usados como estão (ver dúvida D1).

| Situação | HTTP |
|---|---|
| Publicada, com confirmação e roteamento verificados | `202` (corpo: `solicitacaoId`, `pedidoId`, `situacao`) |
| `pedidoId` ou `solicitacaoId` com formato inválido, `cenario` ausente ou desconhecido, JSON malformado | `400` |
| Pedido inexistente | `404` |
| Pedido fora de `ABERTO` (e a solicitação não é repetição legítima), ou pedido sem itens | `409` |
| `solicitacaoId` reutilizado com outro conteúdo (outro `pedidoId` ou outro `cenario`) | `409` |
| Broker indisponível, `nack`, timeout de confirmação ou mensagem devolvida (sem rota) | `503` |

> `202` significa "solicitação aceita e entregue ao broker". **Nunca** significa pagamento aprovado. O resultado só aparece em `GET /pedidos/{id}`.

### 3. Estados do pedido
```
ABERTO --solicitar--> EM_PAGAMENTO --aprovado--> PAGO
                           |--recusado--> ABERTO
```
- `StatusPedido` ganha `EM_PAGAMENTO`.
- Em `EM_PAGAMENTO`, `adicionarItem` falha com `409` (a regra atual `status != ABERTO` já cobre). Os itens e o total não mudam enquanto o pedido espera o resultado.
- Novos métodos de domínio puros: `iniciarPagamento()`, `confirmarPagamento()`, `recusarPagamento()`. `pagar()` é removido.

### 4. Mensagens JSON (versão 1)
Versão 1 no **header AMQP** `versao` (inteiro 1); o corpo não leva o campo. Valores monetários em `BigDecimal`, sem `double`.

Solicitação (`pagamento.solicitar`):
```json
{ "solicitacaoId": "uuid", "pedidoId": "uuid", "valor": 37.80, "cenario": "APROVAR" }
```
Resultado (`pagamento.resultado`):
```json
{ "solicitacaoId": "uuid", "pedidoId": "uuid", "valor": 37.80, "aprovado": true, "motivo": "APROVADO_SIMULADO" }
```
- `RECUSAR` → `aprovado=false`, `motivo="RECUSADO_SIMULADO"`.
- Mensagem sem header `versao` ou com versão desconhecida, ou JSON inválido → DLQ sem alterar estado.

### 5. Publicação confiável e recuperação (sem transação única)
Não existe transação única entre Postgres e RabbitMQ. A consistência vem de estado persistido, idempotência e reenvio.

O serviço de pedidos guarda a tabela `solicitacao_pagamento` no **próprio** Postgres: `solicitacao_id` (PK), `pedido_id`, `valor`, `cenario`, `situacao` (`PENDENTE_PUBLICACAO`, `PUBLICADA`, `APROVADA`, `RECUSADA`).

Fluxo do `POST`:
1. **Transação Postgres:** valida, move o pedido para `EM_PAGAMENTO` e grava a solicitação como `PENDENTE_PUBLICACAO`. Faz commit.
2. Publica no `pagamentos.comandos` com `mandatory=true`, publisher confirms e publisher returns ligados, e `messageId=solicitacaoId`.
3. Só com `ack` do broker **e** sem `return` (roteamento ok), marca `PUBLICADA` e responde `202`.
4. Se a publicação falhar (`nack`, timeout ou `return`): o pedido permanece `EM_PAGAMENTO` e a solicitação `PENDENTE_PUBLICACAO`. A resposta é `503`. O cliente refaz o `POST` com o **mesmo** `solicitacaoId` e o serviço republica. Não há rollback fantasma nem falsa afirmação de atomicidade.
5. Em confirmação com timeout, a mensagem pode ter chegado ao broker. A republicação gera duplicata, e isso é seguro porque o processador deduplica por `solicitacaoId`.

### 6. Repetição persistente (idempotência)
- Mesmo `solicitacaoId` + mesmo `pedidoId` + mesmo `cenario`: não cria nova solicitação nem muda estado de novo. A decisão se baseia na tabela, então vale após reinício.
  - `PENDENTE_PUBLICACAO` → republica e responde `202` após confirmação.
  - `PUBLICADA` ou já concluída → responde `202` sem republicar.
- Mesmo `solicitacaoId` com `pedidoId` ou `cenario` diferente → `409`.
- Outro `solicitacaoId` para pedido já `EM_PAGAMENTO` ou `PAGO` → `409`.

### 7. Resultado conflitante e duplicado
O consumidor de `pedidos.resultados` localiza a solicitação por `solicitacaoId` e confere `pedidoId` e `valor` (por `compareTo`).
- Resultado igual a um já aplicado (duplicata): `ack` sem efeito.
- Resultado conflitante (`aprovado` oposto ao já aplicado, `pedidoId` ou `valor` divergentes, `solicitacaoId` desconhecido): **não altera o pedido** e vai para `pedidos.resultados.erros`.
- Resultado válido com solicitação `PENDENTE_PUBLICACAO` ou `PUBLICADA` (o resultado pode chegar antes de a API marcar `PUBLICADA`) e pedido `EM_PAGAMENTO`: `aprovado` → `PAGO`, senão → `ABERTO`. Aplicado em uma transação Postgres única, atualizando pedido e solicitação juntos.
- O processador simulado é sem estado e determinístico: a mesma solicitação sempre gera o **mesmo** resultado, então duplicatas nunca invertem o desfecho. A deduplicação final é feita pela API.

### 8. Reinício e recuperação
- Estado do pedido e das solicitações fica no Postgres. Filas e mensagens são duráveis. Reiniciar a API não perde solicitações nem resultados.
- Consumidores usam ack após o processamento. A mensagem em andamento na queda volta à fila e é reprocessada de forma idempotente.
- Solicitação `PENDENTE_PUBLICACAO` após a queda: continua `EM_PAGAMENTO` e só se resolve com o reenvio do cliente (ver D3).

### 9. Arquitetura e limites
- `domain` e `application` puros (sem Spring, JPA ou AMQP). Novos ports: `application/port/in/SolicitarPagamento`, `application/port/in/AplicarResultadoPagamento`, `application/port/out/PublicadorSolicitacaoPagamento`, `application/port/out/SolicitacoesPagamento`.
- Adaptadores: REST, JPA, AMQP (publisher e listeners) e a configuração da topologia em `adapters`/`infrastructure`.
- Sem cobrança real. O processador simulado fica em **outro módulo** (`pagamentos-simulador/`, projeto Maven próprio, porta 8081), sem banco, e só fala por mensagens. Os dois declaram a mesma topologia.
- Migração local: bancos já existentes têm o `CHECK pedido_status_check` do Hibernate sem `EM_PAGAMENTO`, e `ddl-auto: update` não o altera. Recriar a constraint (bancos novos já nascem corretos):

```sql
ALTER TABLE pedido DROP CONSTRAINT pedido_status_check;
ALTER TABLE pedido ADD CONSTRAINT pedido_status_check CHECK (status IN ('ABERTO','EM_PAGAMENTO','PAGO','CANCELADO'));
```
- Sem Lombok. Nenhuma dependência nova sem autorização (ver D2).

## Decisões (dúvidas resolvidas)
- D1: `GET /pedidos/{id}` criado nesta etapa. D2: `spring-boot-starter-amqp` na API e no simulador, e `spring-boot-starter-jackson` no simulador, com testes no RabbitMQ do Compose. D3: só reenvio manual. D4: `versao` no header AMQP. D5: simulador em outro módulo.
- D6: `Pedido.pagar()` removido (não havia endpoint HTTP síncrono). D7: pedido sem itens → `409`.
## Plano em etapas pequenas

| # | Etapa | Arquivos | Testes |
|---|---|---|---|
| 1 | Domínio: `EM_PAGAMENTO`, `iniciarPagamento/confirmarPagamento/recusarPagamento`, remover `pagar()` | `StatusPedido`, `Pedido` | `PedidoTest` (transições válidas e inválidas, itens bloqueados), ajustar usos de `pagar()` |
| 2 | Solicitação: modelo, port, caso de uso `SolicitarPagamento` (idempotência, 404, 409) com fakes | `application/...`, `CasosDeUsoConfig` | `SolicitarPagamentoServiceTest` |
| 3 | Persistência da solicitação e `GET /pedidos/{id}` | entity, repository, adapter JPA, controller | `PedidosJpaAdapterIT`, `PedidoControllerIT` |
| 4 | Topologia AMQP (6 recursos com DLQ) e `pom.xml` (se autorizado) | `RabbitTopologyConfig`, `application.yml`, `pom.xml` | teste de topologia no broker do Compose |
| 5 | Publisher com confirm, return e mandatory, e `POST /pagamentos` (202, 400, 404, 409, 503) | adapter AMQP, controller, handler | `PedidoControllerIT` com publicador falso, teste de publicação com broker |
| 6 | Processador simulado com dedupe e resultado | listener de `pagamentos.solicitacoes` | teste unitário e de integração |
| 7 | Consumidor de resultados (aplica, duplicata, conflito → DLQ) | `AplicarResultadoPagamentoService`, listener | `AplicarResultadoPagamentoServiceTest`, integração |
| 8 | Fluxo ponta a ponta e reinício | — | aprova, recusa e repetição ponta a ponta |

## Definição de Pronto
- `.\mvnw.cmd test` verde, com os cenários acima cobertos.
- Topologia criada na primeira subida, com as 4 filas, 2 exchanges e DLQs ligadas.
- `202` só após ack e roteamento. Falhas dão `503` e reenvio idempotente.
- `pagar()` removido. Nenhum segredo novo e nenhum valor monetário em `double`.
