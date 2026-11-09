package br.com.java.lucas.estoque.domain;

import br.com.java.lucas.estoque.domain.exception.HoldAlreadyReleasedException;
import br.com.java.lucas.estoque.domain.exception.InvalidIdentifierException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockHoldTest {

    @Test
    @DisplayName("reserva nova nasce ativa e guarda produto, tipo, quantidade e solicitação")
    void newReservationIsActive() {
        StockHold hold = new StockHold("PROD-1", HoldType.RESERVATION, 5, "REQ-1");

        assertNotNull(hold.getId());
        assertEquals("PROD-1", hold.getProductId());
        assertEquals(HoldType.RESERVATION, hold.getType());
        assertEquals(5, hold.getQuantity());
        assertEquals("REQ-1", hold.getRequestId());
        assertEquals(HoldStatus.ACTIVE, hold.getStatus());
        assertTrue(hold.isActive());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("reserva exige identificador de solicitação")
    void reservationRequiresRequestId(String requestId) {
        assertThrows(InvalidIdentifierException.class,
                () -> new StockHold("PROD-1", HoldType.RESERVATION, 5, requestId));
    }

    @Test
    @DisplayName("bloqueio aceita não ter solicitação associada")
    void blockAllowsMissingRequestId() {
        StockHold hold = new StockHold("PROD-1", HoldType.BLOCK, 5, null);

        assertNull(hold.getRequestId());
        assertTrue(hold.isActive());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("bloqueio com solicitação em branco é rejeitado")
    void blockRejectsBlankRequestId(String requestId) {
        assertThrows(InvalidIdentifierException.class,
                () -> new StockHold("PROD-1", HoldType.BLOCK, 5, requestId));
    }

    @Test
    @DisplayName("liberar marca a retenção como liberada")
    void releaseMarksHoldAsReleased() {
        StockHold hold = new StockHold("PROD-1", HoldType.RESERVATION, 5, "REQ-1");

        hold.release();

        assertEquals(HoldStatus.RELEASED, hold.getStatus());
        assertFalse(hold.isActive());
    }

    @Test
    @DisplayName("liberar duas vezes lança exceção")
    void releaseTwiceIsRejected() {
        StockHold hold = new StockHold("PROD-1", HoldType.RESERVATION, 5, "REQ-1");
        hold.release();

        assertThrows(HoldAlreadyReleasedException.class, hold::release);
    }

    @Test
    @DisplayName("cada retenção recebe um identificador diferente")
    void eachHoldHasUniqueId() {
        StockHold first = new StockHold("PROD-1", HoldType.RESERVATION, 5, "REQ-1");
        StockHold second = new StockHold("PROD-1", HoldType.RESERVATION, 5, "REQ-1");

        assertNotEquals(first.getId(), second.getId());
    }
}