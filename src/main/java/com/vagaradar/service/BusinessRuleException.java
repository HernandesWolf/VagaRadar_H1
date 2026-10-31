package com.vagaradar.service;

/** Violação de regra de negócio (vira HTTP 409). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
