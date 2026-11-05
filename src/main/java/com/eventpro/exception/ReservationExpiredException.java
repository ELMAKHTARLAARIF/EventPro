package com.eventpro.exception;

/**
 * 409 Conflict : paiement tardif, les 10 minutes sont ecoulees.
 */
public class ReservationExpiredException extends RuntimeException {

    public ReservationExpiredException(String message) {
        super(message);
    }
}