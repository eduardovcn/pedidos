# Especificações de Mudanças (.specs)

Este diretório armazena as especificações de cada tarefa, mudança ou funcionalidade a ser implementada no projeto.

---

## Estrutura Padrão de uma Especificação
Cada especificação deve conter obrigatoriamente as seguintes seções:
1. **Contexto:** Explicação do cenário atual, problema ou motivação da mudança.
2. **Tarefa:** Descrição clara e concisa do que deve ser desenvolvido ou ajustado.
3. **Regras:** Requisitos de negócio, restrições arquiteturais e padrões técnicos aplicáveis.
4. **Definição de Pronto (Definition of Done):** Critérios objetivos e verificáveis (ex: testes passando, endpoints validados) para considerar a tarefa concluída.

---

## Diretriz de Contexto para Agentes
- **Economia de Contexto:** Os arquivos desta pasta **NÃO** devem ser carregados automaticamente para o contexto de conversação.
- O agente só deve ler o conteúdo de uma especificação quando for **explicitamente instruído** pelo usuário a consultar determinada spec (ex.: *"leia a spec .specs/001-criar-pedido.md"*).
