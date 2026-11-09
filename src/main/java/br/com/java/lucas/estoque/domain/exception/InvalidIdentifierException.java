package br.com.java.lucas.estoque.domain.exception;

public class InvalidIdentifierException extends StockException {

    public InvalidIdentifierException(String fieldName) {
        super("Identificador inválido: " + fieldName + " não pode ser nulo ou vazio.");
    }
}