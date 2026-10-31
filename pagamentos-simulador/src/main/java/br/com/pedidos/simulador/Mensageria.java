package br.com.pedidos.simulador;

/** Contrato com o serviço de pedidos: nomes da topologia e versão 1 (header "versao"). */
public final class Mensageria {

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

    private Mensageria() {
    }
}