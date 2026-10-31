package com.vagaradar.service;

/** Recurso não encontrado (vira HTTP 404). */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
