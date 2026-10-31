package com.vagaradar.service;

import com.vagaradar.domain.WorkMode;

import java.math.BigDecimal;
import java.util.List;

/** Filtros opcionais da busca de vagas. Qualquer campo nulo é ignorado. */
public record JobFilter(String q, List<String> technology, String location, WorkMode workMode,
                        BigDecimal minSalary) {

    public static JobFilter none() {
        return new JobFilter(null, null, null, null, null);
    }
}
