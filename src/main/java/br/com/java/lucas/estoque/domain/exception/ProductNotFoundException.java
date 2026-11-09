package br.com.java.lucas.estoque.domain.exception;

public class ProductNotFoundException extends StockException {

    public ProductNotFoundException(String productId) {
        super("Produto não encontrado: " + productId + ".");
    }
}