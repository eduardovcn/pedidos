# AGENTS.md - Regras e Diretrizes do Projeto

Este arquivo é a fonte principal de regras e convenções para agentes de IA que atuam neste repositório.

---

## 1. Objetivo do Serviço
- O projeto `api` (`br.com.pedidos:api`) é um serviço de gerenciamento e processamento de pedidos.

---

## 2. Tecnologias e Ferramentas
- **Linguagem:** Java 21.
- **Framework:** Spring Boot.
- **Build Tool:** Maven Wrapper.
  - Todos os comandos de compilação, teste e execução **devem** ser executados via Maven Wrapper:
    - Windows: `.\mvnw.cmd <comando>`
    - Linux/macOS: `./mvnw <comando>`
  - Exemplos:
    - Executar testes: `.\mvnw.cmd test`
    - Compilar: `.\mvnw.cmd compile`
    - Executar aplicação: `.\mvnw.cmd spring-boot:run`

---

## 3. Arquitetura e Estrutura de Código
- **Arquitetura Hexagonal (Ports and Adapters):**
  - O código deve ser organizado separando claramente o núcleo de negócio dos detalhes de infraestrutura e entrega.
- **Pureza das Camadas Internas:**
  - **Domínio (`domain`) e Aplicação (`application`)** devem ser mantidos 100% puros e agnósticos:
    - **PROIBIDO** qualquer import ou anotação de Spring Framework (`@Service`, `@Component`, `@Autowired`, etc.).
    - **PROIBIDO** qualquer import ou anotação de JPA/Hibernate (`@Entity`, `@Table`, `@Id`, etc.).
    - Apenas tipos padrão do Java (POJOs, Records, Interfaces).
  - O Spring e as bibliotecas de persistência/web pertencem exclusivamente à camada de **Adaptadores / Infraestrutura**.

---

## 4. Padrões de Implementação e Dependências
- **Valores Monetários:**
  - Todo e qualquer valor em dinheiro deve utilizar estritamente `BigDecimal` (nunca `float` ou `double`).
- **Sem Lombok:**
  - O uso de Lombok é estritamente **proibido**. Utilize recursos nativos do Java 21 (como `record`, construtores explícitos e métodos padrão).
- **Controle de Dependências:**
  - **Nenhuma dependência nova** pode ser adicionada ao `pom.xml` sem autorização prévia e expressa do usuário.

---

## 5. Fluxo de Trabalho Obrigatório para Agentes
Ao receber uma tarefa, o agente deve seguir rigorosamente as etapas abaixo:
1. **Ler a especificação indicada:** Consultar a spec designada dentro da pasta `.specs/`.
2. **Planejar:** Propor um plano de ação claro e conciso.
3. **Aguardar OK:** Não alterar nem criar arquivos de código antes da aprovação explícita do usuário.
4. **Implementar e Testar:** Após o "OK", implementar e executar os testes automatizados utilizando o Maven Wrapper adequado ao sistema operacional.
5. **Mostrar Diff e Status:** Apresentar o `git status --short`, diff das alterações e resumo das entregas.
