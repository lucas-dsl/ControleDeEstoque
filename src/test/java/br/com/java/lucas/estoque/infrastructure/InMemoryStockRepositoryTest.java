package br.com.java.lucas.estoque.infrastructure;

import br.com.java.lucas.estoque.domain.StockItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryStockRepositoryTest {

    private InMemoryStockRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryStockRepository();
    }

    @Test
    @DisplayName("produto desconhecido retorna vazio")
    void findByIdReturnsEmptyForUnknownProduct() {
        assertTrue(repository.findById("PROD-1").isEmpty());
    }

    @Test
    @DisplayName("produto salvo pode ser encontrado pelo identificador")
    void savedItemCanBeFound() {
        StockItem item = new StockItem("PROD-1");
        item.addStock(10);

        repository.save(item);

        StockItem found = repository.findById("PROD-1").orElseThrow();
        assertEquals("PROD-1", found.getProductId());
        assertEquals(10, found.getTotal());
    }

    @Test
    @DisplayName("produtos diferentes ficam separados")
    void differentProductsAreKeptSeparate() {
        repository.save(new StockItem("PROD-1"));

        assertTrue(repository.findById("PROD-1").isPresent());
        assertTrue(repository.findById("PROD-2").isEmpty());
    }
}