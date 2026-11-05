package com.eventpro.exception;

/**
 * 409 Conflict : le billet a deja ete scanne a l'entree (statut USED).
 */
public class TicketAlreadyUsedException extends RuntimeException {

    public TicketAlreadyUsedException(String message) {
        super(message);
    }
}
