package br.com.java.lucas.estoque.domain;

import br.com.java.lucas.estoque.domain.exception.HoldAlreadyReleasedException;
import br.com.java.lucas.estoque.domain.exception.InvalidIdentifierException;

import java.util.UUID;

public class StockHold {

    private final UUID id;
    private final String productId;
    private final HoldType type;
    private final long quantity;
    private final String requestId;
    private HoldStatus status;

    StockHold(String productId, HoldType type, long quantity, String requestId) {
        validateRequestId(type, requestId);
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.requestId = requestId;
        this.status = HoldStatus.ACTIVE;
    }

    void release() {
        if (status == HoldStatus.RELEASED) {
            throw new HoldAlreadyReleasedException(id);
        }
        status = HoldStatus.RELEASED;
    }

    public boolean isActive() {
        return status == HoldStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public HoldType getType() {
        return type;
    }

    public long getQuantity() {
        return quantity;
    }

    public String getRequestId() {
        return requestId;
    }

    public HoldStatus getStatus() {
        return status;
    }

    private static void validateRequestId(HoldType type, String requestId) {
        boolean missing = requestId == null;
        if (missing && type == HoldType.RESERVATION) {
            throw new InvalidIdentifierException("requestId");
        }
        if (!missing && requestId.isBlank()) {
            throw new InvalidIdentifierException("requestId");
        }
    }
}