package com.vagaradar.web.dto;

import com.vagaradar.domain.ApplicationStatus;
import com.vagaradar.domain.WorkMode;

import java.util.List;
import java.util.Map;

/** Indicadores do painel. Taxas em porcentagem (0–100), calculadas sobre candidaturas efetivamente enviadas. */
public record StatsResponse(
        long totalJobs,
        long totalApplications,
        long submittedApplications,
        long activeApplications,
        long respondedApplications,
        long interviewedApplications,
        long offers,
        double responseRate,
        double interviewRate,
        double offerRate,
        long upcomingInterviews,
        Map<ApplicationStatus, Long> byStatus,
        Map<WorkMode, Long> jobsByWorkMode,
        List<FunnelStep> funnel,
        List<TechCount> topTechnologies) {

    public record FunnelStep(String label, long count) {
    }

    public record TechCount(String name, long count) {
    }
}
