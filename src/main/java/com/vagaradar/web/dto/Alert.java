package com.vagaradar.web.dto;

import java.time.LocalDate;

/** Alerta calculado em tempo real a partir de prazos, entrevistas e follow-ups. */
public record Alert(Type type, Severity severity, String message, LocalDate date,
                    Long jobId, Long applicationId, long daysLeft) {

    public enum Type {
        JOB_DEADLINE, INTERVIEW_UPCOMING, FOLLOW_UP_DUE, STAGE_DUE, STALE_APPLICATION
    }

    public enum Severity {
        CRITICAL, WARNING, INFO
    }
}
