package com.eventpro.exception;

/**
 * 409 Conflict : meme cle d'idempotence reutilisee avec un panier different.
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
