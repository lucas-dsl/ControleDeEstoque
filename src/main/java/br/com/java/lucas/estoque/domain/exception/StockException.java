package br.com.java.lucas.estoque.domain.exception;

public class StockException extends RuntimeException {

    public StockException(String message) {
        super(message);
    }
}