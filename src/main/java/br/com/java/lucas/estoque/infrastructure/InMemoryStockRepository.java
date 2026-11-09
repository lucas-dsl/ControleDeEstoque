package br.com.java.lucas.estoque.infrastructure;

import br.com.java.lucas.estoque.domain.StockItem;
import br.com.java.lucas.estoque.domain.StockRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStockRepository implements StockRepository {

    private final Map<String, StockItem> items = new ConcurrentHashMap<>();

    @Override
    public Optional<StockItem> findById(String productId) {
        return Optional.ofNullable(items.get(productId));
    }

    @Override
    public void save(StockItem item) {
        items.put(item.getProductId(), item);
    }
}