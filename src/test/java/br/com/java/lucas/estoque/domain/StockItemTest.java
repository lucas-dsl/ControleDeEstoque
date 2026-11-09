package br.com.java.lucas.estoque.domain;

import br.com.java.lucas.estoque.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockItemTest {

    private StockItem item;

    @BeforeEach
    void setUp() {
        item = new StockItem("PROD-1");
    }

    @Test
    @DisplayName("produto novo começa com todas as quantidades zeradas")
    void newItemStartsEmpty() {
        assertEquals("PROD-1", item.getProductId());
        assertEquals(0, item.getTotal());
        assertEquals(0, item.getAvailable());
        assertEquals(0, item.getReserved());
        assertEquals(0, item.getBlocked());
    }

    @Test
    @DisplayName("entrada aumenta o total e o disponível")
    void addStockIncreasesTotalAndAvailable() {
        item.addStock(10);

        assertEquals(10, item.getTotal());
        assertEquals(10, item.getAvailable());
    }

    @Test
    @DisplayName("entradas sucessivas acumulam")
    void multipleEntriesAccumulate() {
        item.addStock(10);
        item.addStock(5);

        assertEquals(15, item.getTotal());
        assertEquals(15, item.getAvailable());
    }

    @Test
    @DisplayName("saída com quantidade suficiente diminui o total e o disponível")
    void removeStockDecreasesTotalAndAvailable() {
        item.addStock(10);

        item.removeStock(4);

        assertEquals(6, item.getTotal());
        assertEquals(6, item.getAvailable());
    }

    @Test
    @DisplayName("saída de exatamente todo o disponível é permitida")
    void removeStockAllowsExactlyAvailableQuantity() {
        item.addStock(10);

        item.removeStock(10);

        assertEquals(0, item.getTotal());
        assertEquals(0, item.getAvailable());
    }

    @Test
    @DisplayName("saída maior que o disponível é rejeitada e não altera o estoque")
    void removeStockRejectsMoreThanAvailable() {
        item.addStock(5);

        assertThrows(InsufficientStockException.class, () -> item.removeStock(6));

        assertEquals(5, item.getTotal());
        assertEquals(5, item.getAvailable());
    }

    @Test
    @DisplayName("saída em produto sem estoque é rejeitada")
    void removeStockFromEmptyItemIsRejected() {
        assertThrows(InsufficientStockException.class, () -> item.removeStock(1));
    }
}