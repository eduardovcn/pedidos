package br.com.pedidos.api.infrastructure.messaging;

/** Nomes da topologia RabbitMQ e contrato das mensagens (versão 1, enviada no header "versao"). */
public final class PagamentosMensageria {

    public static final String EXCHANGE_COMANDOS = "pagamentos.comandos";
    public static final String EXCHANGE_EVENTOS = "pagamentos.eventos";
    public static final String CHAVE_SOLICITAR = "pagamento.solicitar";
    public static final String CHAVE_RESULTADO = "pagamento.resultado";
    public static final String FILA_SOLICITACOES = "pagamentos.solicitacoes";
    public static final String FILA_SOLICITACOES_ERROS = "pagamentos.solicitacoes.erros";
    public static final String FILA_RESULTADOS = "pedidos.resultados";
    public static final String FILA_RESULTADOS_ERROS = "pedidos.resultados.erros";
    public static final String HEADER_VERSAO = "versao";
    public static final int VERSAO = 1;

    private PagamentosMensageria() {
    }
}