package com.vagaradar.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApplicationStatusTest {

    @Test
    void fluxoFelizEPermitido() {
        assertTrue(ApplicationStatus.WISHLIST.canTransitionTo(ApplicationStatus.APPLIED));
        assertTrue(ApplicationStatus.APPLIED.canTransitionTo(ApplicationStatus.SCREENING));
        assertTrue(ApplicationStatus.SCREENING.canTransitionTo(ApplicationStatus.INTERVIEWING));
        assertTrue(ApplicationStatus.INTERVIEWING.canTransitionTo(ApplicationStatus.OFFER));
        assertTrue(ApplicationStatus.OFFER.canTransitionTo(ApplicationStatus.ACCEPTED));
    }

    @Test
    void transicoesIncoerentesSaoBloqueadas() {
        assertFalse(ApplicationStatus.WISHLIST.canTransitionTo(ApplicationStatus.OFFER));
        assertFalse(ApplicationStatus.APPLIED.canTransitionTo(ApplicationStatus.ACCEPTED));
        assertFalse(ApplicationStatus.REJECTED.canTransitionTo(ApplicationStatus.APPLIED));
    }

    @Test
    void estadosTerminaisNaoTemSaida() {
        for (ApplicationStatus status : ApplicationStatus.values()) {
            if (status.isTerminal()) {
                assertTrue(status.allowedTransitions().isEmpty(), status + " deveria ser terminal");
            }
        }
    }
}
