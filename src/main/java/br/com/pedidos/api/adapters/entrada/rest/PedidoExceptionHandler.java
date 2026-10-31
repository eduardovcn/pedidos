package br.com.pedidos.api.adapters.entrada.rest;

import br.com.pedidos.api.application.exception.FalhaPublicacaoException;
import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.exception.PedidoSemItensException;
import br.com.pedidos.api.application.exception.SolicitacaoPagamentoConflitoException;
import br.com.pedidos.api.domain.ItemInvalidoException;
import br.com.pedidos.api.domain.PedidoFechadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice
public class PedidoExceptionHandler {

    @ExceptionHandler({ItemInvalidoException.class, PedidoSemItensException.class})
    public ResponseEntity<Map<String, String>> tratarRegraDeNegocio(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("mensagem", ex.getMessage()));
    }

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> tratarNaoEncontrado(PedidoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("mensagem", ex.getMessage()));
    }

    @ExceptionHandler({PedidoFechadoException.class, SolicitacaoPagamentoConflitoException.class})
    public ResponseEntity<Map<String, String>> tratarConflitoDeEstado(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("mensagem", ex.getMessage()));
    }

    @ExceptionHandler(FalhaPublicacaoException.class)
    public ResponseEntity<Map<String, String>> tratarFalhaDePublicacao(FalhaPublicacaoException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("mensagem", "Solicitação registrada, mas a publicação não foi confirmada. "
                        + "Repita o POST com o mesmo solicitacaoId."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> tratarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("mensagem", "Parâmetro inválido: " + ex.getName()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> tratarValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Erro de validação nos campos informados");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("mensagem", mensagem));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> tratarJsonMalformado(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("mensagem", "JSON malformado ou inválido"));
    }
}
