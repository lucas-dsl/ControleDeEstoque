package br.com.java.lucas.estoque.domain.exception;

import java.util.UUID;

public class HoldAlreadyReleasedException extends StockException {

    public HoldAlreadyReleasedException(UUID holdId) {
        super("Reserva/bloqueio " + holdId + " já foi liberado.");
    }
}