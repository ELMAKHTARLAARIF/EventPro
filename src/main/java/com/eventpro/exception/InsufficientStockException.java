package com.eventpro.exception;

/**
 * 409 Conflict : plus assez de places libres pour valider la reservation.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}