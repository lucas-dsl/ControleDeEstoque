package br.com.java.lucas.estoque.application;

import br.com.java.lucas.estoque.domain.StockHold;
import br.com.java.lucas.estoque.domain.StockItem;
import br.com.java.lucas.estoque.domain.StockRepository;
import br.com.java.lucas.estoque.domain.exception.InvalidIdentifierException;
import br.com.java.lucas.estoque.domain.exception.ProductNotFoundException;

import java.util.Objects;
import java.util.UUID;

public class StockService {

    private final StockRepository repository;

    public StockService(StockRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void addStock(String productId, long quantity) {
        validateProductId(productId);
        StockItem item = repository.findById(productId)
                .orElseGet(() -> new StockItem(productId));
        item.addStock(quantity);
        repository.save(item);
    }

    public void removeStock(String productId, long quantity) {
        StockItem item = findExisting(productId);
        item.removeStock(quantity);
        repository.save(item);
    }

    public StockHold reserve(String productId, long quantity, String requestId) {
        StockItem item = findExisting(productId);
        StockHold hold = item.reserve(quantity, requestId);
        repository.save(item);
        return hold;
    }

    public StockHold block(String productId, long quantity, String requestId) {
        StockItem item = findExisting(productId);
        StockHold hold = item.block(quantity, requestId);
        repository.save(item);
        return hold;
    }

    public void release(String productId, UUID holdId) {
        StockItem item = findExisting(productId);
        item.release(holdId);
        repository.save(item);
    }

    public StockSummary getStock(String productId) {
        return StockSummary.from(findExisting(productId));
    }

    public StockHold getHold(String productId, UUID holdId) {
        return findExisting(productId).getHold(holdId);
    }

    private StockItem findExisting(String productId) {
        validateProductId(productId);
        return repository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private static void validateProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new InvalidIdentifierException("productId");
        }
    }
}