package br.com.java.lucas.estoque.domain;

import br.com.java.lucas.estoque.domain.exception.InsufficientStockException;
import br.com.java.lucas.estoque.domain.exception.InvalidIdentifierException;
import br.com.java.lucas.estoque.domain.exception.InvalidQuantityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import br.com.java.lucas.estoque.domain.exception.HoldAlreadyReleasedException;
import br.com.java.lucas.estoque.domain.exception.HoldNotFoundException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockItemHoldsTest {

    private StockItem item;

    @BeforeEach
    void setUp() {
        item = new StockItem("PROD-1");
        item.addStock(10);
    }

    private void assertStock(long total, long available, long reserved, long blocked) {
        assertEquals(total, item.getTotal(), "total");
        assertEquals(available, item.getAvailable(), "disponível");
        assertEquals(reserved, item.getReserved(), "reservado");
        assertEquals(blocked, item.getBlocked(), "bloqueado");
    }

    @Test
    @DisplayName("reserva move do disponível para o reservado, sem alterar o total")
    void reserveMovesFromAvailableToReserved() {
        StockHold hold = item.reserve(4, "REQ-1");

        assertStock(10, 6, 4, 0);
        assertEquals(HoldType.RESERVATION, hold.getType());
        assertEquals(4, hold.getQuantity());
        assertEquals("PROD-1", hold.getProductId());
        assertEquals("REQ-1", hold.getRequestId());
        assertTrue(hold.isActive());
    }

    @Test
    @DisplayName("bloqueio move do disponível para o bloqueado, sem alterar o total")
    void blockMovesFromAvailableToBlocked() {
        StockHold hold = item.block(3, "REQ-2");

        assertStock(10, 7, 0, 3);
        assertEquals(HoldType.BLOCK, hold.getType());
        assertEquals(3, hold.getQuantity());
        assertEquals("REQ-2", hold.getRequestId());
    }

    @Test
    @DisplayName("bloqueio pode ser feito sem solicitação associada")
    void blockWithoutRequestIsAllowed() {
        StockHold hold = item.block(3, null);

        assertNull(hold.getRequestId());
        assertStock(10, 7, 0, 3);
    }

    @Test
    @DisplayName("permite várias reservas no mesmo produto")
    void allowsMultipleReservations() {
        StockHold first = item.reserve(2, "REQ-1");
        StockHold second = item.reserve(3, "REQ-2");

        assertStock(10, 5, 5, 0);
        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    @DisplayName("permite vários bloqueios no mesmo produto")
    void allowsMultipleBlocks() {
        StockHold first = item.block(2, "REQ-1");
        StockHold second = item.block(1, null);

        assertStock(10, 7, 0, 3);
        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    @DisplayName("reserva e bloqueio juntos")
    void reservationAndBlockTogether() {
        item.reserve(3, "REQ-1");
        item.block(2, "REQ-2");

        assertStock(10, 5, 3, 2);
    }

    @Test
    @DisplayName("é possível reservar exatamente todo o disponível")
    void reserveEntireAvailableQuantity() {
        item.reserve(10, "REQ-1");

        assertStock(10, 0, 10, 0);
    }

    @Test
    @DisplayName("reserva maior que o disponível é rejeitada e não altera o estoque")
    void reserveRejectsMoreThanAvailable() {
        item.reserve(4, "REQ-1");

        assertThrows(InsufficientStockException.class, () -> item.reserve(7, "REQ-2"));

        assertStock(10, 6, 4, 0);
    }

    @Test
    @DisplayName("bloqueio maior que o disponível é rejeitado e não altera o estoque")
    void blockRejectsMoreThanAvailable() {
        assertThrows(InsufficientStockException.class, () -> item.block(11, "REQ-1"));

        assertStock(10, 10, 0, 0);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -100})
    @DisplayName("reserva com quantidade zero ou negativa é rejeitada")
    void reserveRejectsNonPositiveQuantity(long quantity) {
        assertThrows(InvalidQuantityException.class, () -> item.reserve(quantity, "REQ-1"));

        assertStock(10, 10, 0, 0);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -100})
    @DisplayName("bloqueio com quantidade zero ou negativa é rejeitado")
    void blockRejectsNonPositiveQuantity(long quantity) {
        assertThrows(InvalidQuantityException.class, () -> item.block(quantity, "REQ-1"));

        assertStock(10, 10, 0, 0);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("reserva sem identificador de solicitação é rejeitada e não altera o estoque")
    void reserveRejectsInvalidRequestId(String requestId) {
        assertThrows(InvalidIdentifierException.class, () -> item.reserve(4, requestId));

        assertStock(10, 10, 0, 0);
    }

    @Test
    @DisplayName("saída não pode retirar o que está reservado ou bloqueado")
    void removeStockCannotTakeHeldQuantities() {
        item.reserve(6, "REQ-1");
        item.block(2, "REQ-2");

        assertThrows(InsufficientStockException.class, () -> item.removeStock(3));
        assertStock(10, 2, 6, 2);

        item.removeStock(2);
        assertStock(8, 0, 6, 2);
    }

    @Test
    @DisplayName("liberar reserva devolve ao disponível, sem alterar o total")
    void releaseReservationReturnsQuantityToAvailable() {
        StockHold hold = item.reserve(4, "REQ-1");

        item.release(hold.getId());

        assertStock(10, 10, 0, 0);
        assertEquals(HoldStatus.RELEASED, item.getHold(hold.getId()).getStatus());
    }

    @Test
    @DisplayName("liberar bloqueio devolve ao disponível, sem alterar o total")
    void releaseBlockReturnsQuantityToAvailable() {
        StockHold hold = item.block(3, null);

        item.release(hold.getId());

        assertStock(10, 10, 0, 0);
        assertEquals(HoldStatus.RELEASED, item.getHold(hold.getId()).getStatus());
    }

    @Test
    @DisplayName("liberar uma retenção não afeta as outras")
    void releaseOnlyAffectsThatHold() {
        StockHold first = item.reserve(3, "REQ-1");
        StockHold second = item.reserve(2, "REQ-2");

        item.release(first.getId());

        assertStock(10, 8, 2, 0);
        assertTrue(item.getHold(second.getId()).isActive());
    }

    @Test
    @DisplayName("liberar retenção inexistente é rejeitado e não altera o estoque")
    void releaseUnknownHoldIsRejected() {
        item.reserve(4, "REQ-1");

        assertThrows(HoldNotFoundException.class, () -> item.release(UUID.randomUUID()));

        assertStock(10, 6, 4, 0);
    }

    @Test
    @DisplayName("liberar com identificador nulo é rejeitado")
    void releaseNullIdIsRejected() {
        assertThrows(InvalidIdentifierException.class, () -> item.release(null));
    }

    @Test
    @DisplayName("liberar a mesma retenção duas vezes é rejeitado e não devolve quantidade em dobro")
    void releaseTwiceIsRejected() {
        StockHold hold = item.reserve(4, "REQ-1");
        item.release(hold.getId());

        assertThrows(HoldAlreadyReleasedException.class, () -> item.release(hold.getId()));

        assertStock(10, 10, 0, 0);
    }

    @Test
    @DisplayName("retenção de outro produto não pode ser liberada")
    void releaseHoldFromOtherProductIsRejected() {
        StockItem other = new StockItem("PROD-2");
        other.addStock(10);
        StockHold hold = item.reserve(4, "REQ-1");

        assertThrows(HoldNotFoundException.class, () -> other.release(hold.getId()));

        assertEquals(10, other.getAvailable());
        assertStock(10, 6, 4, 0);
    }

    @Test
    @DisplayName("consulta devolve a retenção pelo identificador")
    void getHoldReturnsHoldById() {
        StockHold hold = item.reserve(4, "REQ-1");

        StockHold found = item.getHold(hold.getId());

        assertEquals(hold.getId(), found.getId());
        assertEquals(4, found.getQuantity());
        assertEquals("REQ-1", found.getRequestId());
    }

    @Test
    @DisplayName("consulta de retenção inexistente é rejeitada")
    void getUnknownHoldIsRejected() {
        assertThrows(HoldNotFoundException.class, () -> item.getHold(UUID.randomUUID()));
    }
}