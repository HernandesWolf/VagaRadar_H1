package com.vagaradar.domain;

/** Nível de domínio de uma competência (do usuário) ou nível mínimo exigido (pela vaga). */
public enum SkillLevel {
    BEGINNER(1), INTERMEDIATE(2), ADVANCED(3);

    private final int rank;

    SkillLevel(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
