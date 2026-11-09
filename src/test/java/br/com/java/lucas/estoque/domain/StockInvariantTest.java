package br.com.java.lucas.estoque.domain;

import br.com.java.lucas.estoque.domain.exception.StockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockInvariantTest {

    private StockItem item;

    @BeforeEach
    void setUp() {
        item = new StockItem("PROD-1");
    }

    @Test
    @DisplayName("reservar, bloquear e liberar não alteram o total; só entrada e saída alteram")
    void onlyEntriesAndExitsChangeTotal() {
        item.addStock(20);
        assertEquals(20, item.getTotal());

        StockHold reservation = item.reserve(5, "REQ-1");
        assertEquals(20, item.getTotal());

        StockHold block = item.block(3, "REQ-2");
        assertEquals(20, item.getTotal());

        item.release(reservation.getId());
        assertEquals(20, item.getTotal());

        item.release(block.getId());
        assertEquals(20, item.getTotal());

        item.removeStock(8);
        assertEquals(12, item.getTotal());

        item.addStock(8);
        assertEquals(20, item.getTotal());
    }

    @Test
    @DisplayName("reservado e bloqueado batem com a soma das retenções ativas")
    void heldQuantitiesMatchActiveHolds() {
        item.addStock(50);
        List<StockHold> holds = new ArrayList<>();
        holds.add(item.reserve(10, "A"));
        holds.add(item.reserve(5, "B"));
        holds.add(item.block(7, null));
        holds.add(item.block(3, "C"));

        item.release(holds.get(1).getId());
        item.release(holds.get(2).getId());
        item.removeStock(4);

        assertEquals(activeQuantity(holds, HoldType.RESERVATION), item.getReserved());
        assertEquals(activeQuantity(holds, HoldType.BLOCK), item.getBlocked());
        assertEquals(46, item.getTotal());
        assertEquals(33, item.getAvailable());
        assertEquals(10, item.getReserved());
        assertEquals(3, item.getBlocked());
    }

    @Test
    @DisplayName("nenhuma operação rejeitada altera o estoque")
    void rejectedOperationsLeaveStockUntouched() {
        item.addStock(10);
        item.reserve(4, "REQ-1");

        assertThrows(StockException.class, () -> item.removeStock(7));
        assertThrows(StockException.class, () -> item.reserve(7, "REQ-2"));
        assertThrows(StockException.class, () -> item.block(7, null));
        assertThrows(StockException.class, () -> item.addStock(0));
        assertThrows(StockException.class, () -> item.reserve(1, null));
        assertThrows(StockException.class, () -> item.release(UUID.randomUUID()));

        assertEquals(10, item.getTotal());
        assertEquals(6, item.getAvailable());
        assertEquals(4, item.getReserved());
        assertEquals(0, item.getBlocked());
    }

    private long activeQuantity(List<StockHold> holds, HoldType type) {
        return holds.stream()
                .filter(hold -> hold.getType() == type && hold.isActive())
                .mapToLong(StockHold::getQuantity)
                .sum();
    }
}