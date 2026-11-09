package br.com.java.lucas.estoque.domain;

import br.com.java.lucas.estoque.domain.exception.HoldNotFoundException;
import br.com.java.lucas.estoque.domain.exception.InsufficientStockException;
import br.com.java.lucas.estoque.domain.exception.InvalidIdentifierException;
import br.com.java.lucas.estoque.domain.exception.InvalidQuantityException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StockItem {

    private final String productId;
    private long available;
    private long reserved;
    private long blocked;
    private final Map<UUID, StockHold> holds = new HashMap<>();

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

    public StockHold reserve(long quantity, String requestId) {
        return createHold(HoldType.RESERVATION, quantity, requestId);
    }

    public StockHold block(long quantity, String requestId) {
        return createHold(HoldType.BLOCK, quantity, requestId);
    }

    public StockHold getHold(UUID holdId) {
        if (holdId == null) {
            throw new InvalidIdentifierException("holdId");
        }
        StockHold hold = holds.get(holdId);
        if (hold == null) {
            throw new HoldNotFoundException(holdId, productId);
        }
        return hold;
    }

    public void release(UUID holdId) {
        StockHold hold = getHold(holdId);
        hold.release(); // lança se já liberada, antes de qualquer quantidade mudar
        available += hold.getQuantity();
        addHeld(hold.getType(), -hold.getQuantity());
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

    private StockHold createHold(HoldType type, long quantity, String requestId) {
        validateQuantity(quantity);
        StockHold hold = new StockHold(productId, type, quantity, requestId);
        if (quantity > available) {
            throw new InsufficientStockException(quantity, available);
        }
        available -= quantity;
        addHeld(type, quantity);
        holds.put(hold.getId(), hold);
        return hold;
    }

    private void addHeld(HoldType type, long delta) {
        if (type == HoldType.RESERVATION) {
            reserved += delta;
        } else {
            blocked += delta;
        }
    }

    private void validateQuantity(long quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }
    }
}