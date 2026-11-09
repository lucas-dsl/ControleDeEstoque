package br.com.java.lucas.estoque.application;

import br.com.java.lucas.estoque.domain.HoldStatus;
import br.com.java.lucas.estoque.domain.HoldType;
import br.com.java.lucas.estoque.domain.StockHold;
import br.com.java.lucas.estoque.domain.exception.InsufficientStockException;
import br.com.java.lucas.estoque.infrastructure.InMemoryStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import br.com.java.lucas.estoque.domain.exception.HoldAlreadyReleasedException;
import br.com.java.lucas.estoque.domain.exception.HoldNotFoundException;
import br.com.java.lucas.estoque.domain.exception.InvalidIdentifierException;
import br.com.java.lucas.estoque.domain.exception.InvalidQuantityException;
import br.com.java.lucas.estoque.domain.exception.ProductNotFoundException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockServiceTest {

    private StockService service;

    @BeforeEach
    void setUp() {
        service = new StockService(new InMemoryStockRepository());
    }

    private void assertStock(String productId, long total, long available, long reserved, long blocked) {
        StockSummary summary = service.getStock(productId);
        assertEquals(productId, summary.productId());
        assertEquals(total, summary.total(), "total");
        assertEquals(available, summary.available(), "disponível");
        assertEquals(reserved, summary.reserved(), "reservado");
        assertEquals(blocked, summary.blocked(), "bloqueado");
    }

    @Test
    @DisplayName("primeira entrada cria o produto")
    void firstEntryCreatesProduct() {
        service.addStock("PROD-1", 10);

        assertStock("PROD-1", 10, 10, 0, 0);
    }

    @Test
    @DisplayName("entradas seguintes acumulam no produto existente")
    void laterEntriesAccumulate() {
        service.addStock("PROD-1", 10);
        service.addStock("PROD-1", 5);

        assertStock("PROD-1", 15, 15, 0, 0);
    }

    @Test
    @DisplayName("saída diminui o total e o disponível")
    void removeStockDecreasesTotalAndAvailable() {
        service.addStock("PROD-1", 10);

        service.removeStock("PROD-1", 4);

        assertStock("PROD-1", 6, 6, 0, 0);
    }

    @Test
    @DisplayName("saída acima do disponível é rejeitada e não altera o estoque")
    void removeStockRejectsMoreThanAvailable() {
        service.addStock("PROD-1", 10);
        service.reserve("PROD-1", 6, "REQ-1");

        assertThrows(InsufficientStockException.class, () -> service.removeStock("PROD-1", 5));

        assertStock("PROD-1", 10, 4, 6, 0);
    }

    @Test
    @DisplayName("reserva devolve a retenção com produto, solicitação, tipo e quantidade")
    void reserveReturnsHold() {
        service.addStock("PROD-1", 10);

        StockHold hold = service.reserve("PROD-1", 4, "REQ-1");

        assertEquals("PROD-1", hold.getProductId());
        assertEquals("REQ-1", hold.getRequestId());
        assertEquals(HoldType.RESERVATION, hold.getType());
        assertEquals(4, hold.getQuantity());
        assertStock("PROD-1", 10, 6, 4, 0);
    }

    @Test
    @DisplayName("bloqueio devolve a retenção e pode não ter solicitação")
    void blockReturnsHold() {
        service.addStock("PROD-1", 10);

        StockHold hold = service.block("PROD-1", 3, null);

        assertEquals(HoldType.BLOCK, hold.getType());
        assertEquals(3, hold.getQuantity());
        assertStock("PROD-1", 10, 7, 0, 3);
    }

    @Test
    @DisplayName("liberar reserva devolve a quantidade ao disponível")
    void releaseReservation() {
        service.addStock("PROD-1", 10);
        StockHold hold = service.reserve("PROD-1", 4, "REQ-1");

        service.release("PROD-1", hold.getId());

        assertStock("PROD-1", 10, 10, 0, 0);
        assertEquals(HoldStatus.RELEASED, service.getHold("PROD-1", hold.getId()).getStatus());
    }

    @Test
    @DisplayName("liberar bloqueio devolve a quantidade ao disponível")
    void releaseBlock() {
        service.addStock("PROD-1", 10);
        StockHold hold = service.block("PROD-1", 3, "REQ-1");

        service.release("PROD-1", hold.getId());

        assertStock("PROD-1", 10, 10, 0, 0);
    }

    @Test
    @DisplayName("consulta de retenção devolve a reserva pelo identificador")
    void getHoldReturnsHoldById() {
        service.addStock("PROD-1", 10);
        StockHold hold = service.reserve("PROD-1", 4, "REQ-1");

        StockHold found = service.getHold("PROD-1", hold.getId());

        assertEquals(hold.getId(), found.getId());
        assertEquals(4, found.getQuantity());
        assertEquals("REQ-1", found.getRequestId());
    }

    @Test
    @DisplayName("produtos diferentes têm estoques independentes")
    void productsAreIndependent() {
        service.addStock("PROD-1", 10);
        service.addStock("PROD-2", 5);

        service.reserve("PROD-1", 4, "REQ-1");

        assertStock("PROD-1", 10, 6, 4, 0);
        assertStock("PROD-2", 5, 5, 0, 0);
    }

    @Test
    @DisplayName("fluxo completo: entrada, reserva, bloqueio, saída, liberações e consulta")
    void fullFlow() {
        service.addStock("PROD-1", 20);
        StockHold reservation = service.reserve("PROD-1", 5, "REQ-1");
        StockHold block = service.block("PROD-1", 3, null);
        assertStock("PROD-1", 20, 12, 5, 3);

        assertThrows(InsufficientStockException.class, () -> service.removeStock("PROD-1", 13));
        service.removeStock("PROD-1", 12);
        assertStock("PROD-1", 8, 0, 5, 3);

        service.release("PROD-1", reservation.getId());
        service.release("PROD-1", block.getId());
        assertStock("PROD-1", 8, 8, 0, 0);
    }

    @Test
    @DisplayName("operações em produto inexistente lançam ProductNotFoundException")
    void operationsOnUnknownProductAreRejected() {
        UUID holdId = UUID.randomUUID();

        assertAll(
                () -> assertThrows(ProductNotFoundException.class, () -> service.removeStock("PROD-X", 1)),
                () -> assertThrows(ProductNotFoundException.class, () -> service.reserve("PROD-X", 1, "REQ-1")),
                () -> assertThrows(ProductNotFoundException.class, () -> service.block("PROD-X", 1, null)),
                () -> assertThrows(ProductNotFoundException.class, () -> service.release("PROD-X", holdId)),
                () -> assertThrows(ProductNotFoundException.class, () -> service.getStock("PROD-X")),
                () -> assertThrows(ProductNotFoundException.class, () -> service.getHold("PROD-X", holdId))
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("identificador de produto nulo, vazio ou em branco é rejeitado em todas as operações")
    void operationsWithInvalidProductIdAreRejected(String productId) {
        UUID holdId = UUID.randomUUID();

        assertAll(
                () -> assertThrows(InvalidIdentifierException.class, () -> service.addStock(productId, 1)),
                () -> assertThrows(InvalidIdentifierException.class, () -> service.removeStock(productId, 1)),
                () -> assertThrows(InvalidIdentifierException.class, () -> service.reserve(productId, 1, "REQ-1")),
                () -> assertThrows(InvalidIdentifierException.class, () -> service.block(productId, 1, null)),
                () -> assertThrows(InvalidIdentifierException.class, () -> service.release(productId, holdId)),
                () -> assertThrows(InvalidIdentifierException.class, () -> service.getStock(productId)),
                () -> assertThrows(InvalidIdentifierException.class, () -> service.getHold(productId, holdId))
        );
    }

    @Test
    @DisplayName("reserva sem solicitação é rejeitada e não altera o estoque")
    void reserveWithoutRequestIdIsRejected() {
        service.addStock("PROD-1", 10);

        assertThrows(InvalidIdentifierException.class, () -> service.reserve("PROD-1", 4, null));

        assertStock("PROD-1", 10, 10, 0, 0);
    }

    @Test
    @DisplayName("liberar com identificador de retenção nulo é rejeitado")
    void releaseWithNullHoldIdIsRejected() {
        service.addStock("PROD-1", 10);

        assertThrows(InvalidIdentifierException.class, () -> service.release("PROD-1", null));
    }

    @Test
    @DisplayName("entrada inválida em produto novo não cria o produto")
    void invalidFirstEntryDoesNotCreateProduct() {
        assertThrows(InvalidQuantityException.class, () -> service.addStock("PROD-NEW", 0));

        assertThrows(ProductNotFoundException.class, () -> service.getStock("PROD-NEW"));
    }

    @Test
    @DisplayName("liberar retenção inexistente é rejeitado e não altera o estoque")
    void releaseUnknownHoldIsRejected() {
        service.addStock("PROD-1", 10);
        service.reserve("PROD-1", 4, "REQ-1");

        assertThrows(HoldNotFoundException.class, () -> service.release("PROD-1", UUID.randomUUID()));

        assertStock("PROD-1", 10, 6, 4, 0);
    }

    @Test
    @DisplayName("liberar a mesma retenção duas vezes é rejeitado e não devolve quantidade em dobro")
    void releaseTwiceIsRejected() {
        service.addStock("PROD-1", 10);
        StockHold hold = service.reserve("PROD-1", 4, "REQ-1");
        service.release("PROD-1", hold.getId());

        assertThrows(HoldAlreadyReleasedException.class, () -> service.release("PROD-1", hold.getId()));

        assertStock("PROD-1", 10, 10, 0, 0);
    }

    @Test
    @DisplayName("retenção de um produto não pode ser liberada por outro produto")
    void releaseHoldThroughOtherProductIsRejected() {
        service.addStock("PROD-1", 10);
        service.addStock("PROD-2", 10);
        StockHold hold = service.reserve("PROD-1", 4, "REQ-1");

        assertThrows(HoldNotFoundException.class, () -> service.release("PROD-2", hold.getId()));

        assertStock("PROD-1", 10, 6, 4, 0);
        assertStock("PROD-2", 10, 10, 0, 0);
    }
}