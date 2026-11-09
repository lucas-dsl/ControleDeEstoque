package br.com.java.lucas.estoque.domain.exception;

import java.util.UUID;

public class HoldNotFoundException extends StockException {

    public HoldNotFoundException(UUID holdId, String productId) {
        super("Reserva/bloqueio " + holdId + " não encontrado para o produto " + productId + ".");
    }
}