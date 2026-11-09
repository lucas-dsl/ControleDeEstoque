package br.com.java.lucas.estoque.domain.exception;

public class InsufficientStockException extends StockException {

    public InsufficientStockException(long requested, long available) {
        super("Quantidade insuficiente: solicitado " + requested + ", disponível " + available + ".");
    }
}