package br.com.java.lucas.estoque.domain;

import br.com.java.lucas.estoque.domain.exception.InsufficientStockException;
import br.com.java.lucas.estoque.domain.exception.InvalidIdentifierException;
import br.com.java.lucas.estoque.domain.exception.InvalidQuantityException;

public class StockItem {

    private final String productId;
    private long available;
    private long reserved;
    private long blocked;

    public StockItem(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new InvalidIdentifierException("productId");
        }
        this.productId = productId;
    }

    public void addStock(long quantity) {
        validateQuantity(quantity);
        available += quantity;
    }

    public void removeStock(long quantity) {
        validateQuantity(quantity);
        if (quantity > available) {
            throw new InsufficientStockException(quantity, available);
        }
        available -= quantity;
    }

    public String getProductId() {
        return productId;
    }

    public long getTotal() {
        return available + reserved + blocked;
    }

    public long getAvailable() {
        return available;
    }

    public long getReserved() {
        return reserved;
    }

    public long getBlocked() {
        return blocked;
    }

    private void validateQuantity(long quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }
    }
}