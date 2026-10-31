package com.vagaradar.domain;

/** Situação de uma etapa do processo seletivo. */
public enum StageStatus {
    PENDING, IN_PROGRESS, PASSED, FAILED, SKIPPED;

    public boolean isOpen() {
        return this == PENDING || this == IN_PROGRESS;
    }
}
