# pagamentos

Serviço de Pagamentos **simulado**. Projeto Maven independente (não é módulo do pom de Pedidos), Java 21, Spring Boot 4.1.1, porta **8081**, banco próprio `pagamentos`. Não há cobrança real, aleatoriedade, cartão nem SDK financeiro.

## O que faz
1. Consome `pagamentos.solicitacoes` (JSON, header AMQP `versao` = 1).
2. Valida versão, IDs, valor positivo (até 2 casas) e cenário (`APROVAR` ou `RECUSAR`).
3. Grava **uma** decisão por `solicitacaoId` (chave primária em `decisao_pagamento`). Mesma chave com outro pedido, valor ou cenário é conflito.
4. Publica o resultado em `pagamentos.eventos` com a chave `pagamento.resultado` (persistente, `mandatory`, confirmação correlacionada e detecção de retorno). O ack da solicitação só vem depois de salvar e de o resultado ser confirmado e roteado.
5. Recebendo a mesma solicitação de novo, reutiliza a decisão salva e republica o mesmo resultado.

## Banco
Banco `pagamentos` no Postgres do Compose (`localhost:5433`), mesma credencial didática local. Não lê tabelas de Pedidos. Criação única:

```
docker exec pedidos-postgres psql -U pedidos -d pedidos -c "CREATE DATABASE pagamentos"
```

A tabela `decisao_pagamento` é criada na subida (`ddl-auto: update`).

## Falhas e filas de erro
A topologia é a mesma de Pedidos (declaração idempotente, argumentos idênticos). Falha de processamento, após 3 tentativas, rejeita sem reenfileirar e a mensagem vai para `pagamentos.solicitacoes.erros`. Não há loop infinito. Uma falha de publicação deixa a decisão salva; reprocessar a mesma solicitação republica o mesmo resultado. A operação de recuperação das filas de erro não é tratada aqui.

## Executar e testar
```
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```
Os testes de integração usam o Postgres e o RabbitMQ do Compose. **Pare a API de Pedidos e não rode `pagamentos-simulador` ao mesmo tempo**: ambos disputam a mesma fila.
