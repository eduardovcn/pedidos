package br.com.pedidos.api.application.port.out;

import java.util.function.Supplier;

public interface Transacao {

    <T> T executar(Supplier<T> trabalho);
}