package com.vagaradar.service;

import com.vagaradar.domain.ApplicationStatus;

/** Tentativa de mover a candidatura para um estado que a máquina de estados não permite. */
public class InvalidStatusTransitionException extends BusinessRuleException {
    public InvalidStatusTransitionException(ApplicationStatus from, ApplicationStatus to) {
        super("Transição não permitida: " + from + " → " + to
                + ". Permitidas a partir de " + from + ": " + from.allowedTransitions());
    }
}
