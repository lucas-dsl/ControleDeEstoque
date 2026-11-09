package br.com.java.lucas.estoque.application;

import br.com.java.lucas.estoque.domain.StockHold;
import br.com.java.lucas.estoque.domain.StockItem;
import br.com.java.lucas.estoque.domain.StockRepository;
import br.com.java.lucas.estoque.domain.exception.InvalidIdentifierException;
import br.com.java.lucas.estoque.domain.exception.ProductNotFoundException;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class StockService {

    private final StockRepository repository;
    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    public StockService(StockRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void addStock(String productId, long quantity) {
        synchronized (lockFor(productId)) {
            StockItem item = repository.findById(productId)
                    .orElseGet(() -> new StockItem(productId));
            item.addStock(quantity);
            repository.save(item);
        }
    }

    public void removeStock(String productId, long quantity) {
        synchronized (lockFor(productId)) {
            StockItem item = findExisting(productId);
            item.removeStock(quantity);
            repository.save(item);
        }
    }

    public StockHold reserve(String productId, long quantity, String requestId) {
        synchronized (lockFor(productId)) {
            StockItem item = findExisting(productId);
            StockHold hold = item.reserve(quantity, requestId);
            repository.save(item);
            return hold;
        }
    }

    public StockHold block(String productId, long quantity, String requestId) {
        synchronized (lockFor(productId)) {
            StockItem item = findExisting(productId);
            StockHold hold = item.block(quantity, requestId);
            repository.save(item);
            return hold;
        }
    }

    public void release(String productId, UUID holdId) {
        synchronized (lockFor(productId)) {
            StockItem item = findExisting(productId);
            item.release(holdId);
            repository.save(item);
        }
    }

    public StockSummary getStock(String productId) {
        synchronized (lockFor(productId)) {
            return StockSummary.from(findExisting(productId));
        }
    }

    public StockHold getHold(String productId, UUID holdId) {
        synchronized (lockFor(productId)) {
            return findExisting(productId).getHold(holdId);
        }
    }

    private Object lockFor(String productId) {
        validateProductId(productId);
        return locks.computeIfAbsent(productId, id -> new Object());
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