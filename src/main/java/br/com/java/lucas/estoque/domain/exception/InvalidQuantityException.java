package br.com.java.lucas.estoque.domain.exception;

public class InvalidQuantityException extends StockException {

    public InvalidQuantityException(long quantity) {
        super("Quantidade inválida: " + quantity + ". Deve ser maior que zero.");
    }
}