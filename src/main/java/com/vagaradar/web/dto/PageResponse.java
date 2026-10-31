package com.vagaradar.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Envelope de paginação estável para a API (evita serializar PageImpl diretamente). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
