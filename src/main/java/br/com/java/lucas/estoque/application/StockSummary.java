package br.com.java.lucas.estoque.application;

import br.com.java.lucas.estoque.domain.StockItem;

public record StockSummary(String productId, long total, long available, long reserved, long blocked) {

    static StockSummary from(StockItem item) {
        return new StockSummary(
                item.getProductId(),
                item.getTotal(),
                item.getAvailable(),
                item.getReserved(),
                item.getBlocked());
    }
}