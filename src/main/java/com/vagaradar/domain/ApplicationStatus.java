package com.vagaradar.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Situação de uma candidatura. As transições permitidas formam uma pequena máquina de estados,
 * evitando incoerências como "aceitar" uma proposta de uma candidatura que nunca foi enviada.
 */
public enum ApplicationStatus {
    WISHLIST, APPLIED, SCREENING, INTERVIEWING, OFFER, ACCEPTED, REJECTED, WITHDRAWN;

    /** Estados para os quais é permitido ir a partir deste. */
    public Set<ApplicationStatus> allowedTransitions() {
        switch (this) {
            case WISHLIST:
                return EnumSet.of(APPLIED, WITHDRAWN);
            case APPLIED:
                return EnumSet.of(SCREENING, INTERVIEWING, REJECTED, WITHDRAWN);
            case SCREENING:
                return EnumSet.of(INTERVIEWING, REJECTED, WITHDRAWN);
            case INTERVIEWING:
                return EnumSet.of(OFFER, REJECTED, WITHDRAWN);
            case OFFER:
                return EnumSet.of(ACCEPTED, REJECTED, WITHDRAWN);
            default:
                return EnumSet.noneOf(ApplicationStatus.class);
        }
    }

    public boolean canTransitionTo(ApplicationStatus target) {
        return allowedTransitions().contains(target);
    }

    /** Candidatura ainda em andamento (já enviada e sem desfecho). */
    public boolean isActive() {
        return this == APPLIED || this == SCREENING || this == INTERVIEWING || this == OFFER;
    }

    public boolean isTerminal() {
        return this == ACCEPTED || this == REJECTED || this == WITHDRAWN;
    }
}
