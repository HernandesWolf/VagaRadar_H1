package com.vagaradar.service;

import com.vagaradar.domain.ApplicationStatus;
import com.vagaradar.domain.InterviewStatus;
import com.vagaradar.domain.JobApplication;
import com.vagaradar.domain.JobOpportunity;
import com.vagaradar.domain.JobRequirement;
import com.vagaradar.domain.WorkMode;
import com.vagaradar.repository.InterviewRepository;
import com.vagaradar.repository.JobApplicationRepository;
import com.vagaradar.repository.JobOpportunityRepository;
import com.vagaradar.web.dto.StatsResponse;
import com.vagaradar.web.dto.StatsResponse.FunnelStep;
import com.vagaradar.web.dto.StatsResponse.TechCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Indicadores do processo de busca.
 *
 * <p>Definições (sobre candidaturas efetivamente <b>enviadas</b>, isto é, com data de candidatura):
 * <ul>
 *   <li><b>Resposta</b>: a empresa reagiu (triagem, entrevista, proposta ou recusa) ou houve entrevista registrada.</li>
 *   <li><b>Entrevista</b>: status ENTREVISTANDO/PROPOSTA/ACEITA ou ao menos uma entrevista registrada.</li>
 *   <li><b>Proposta</b>: status PROPOSTA ou ACEITA.</li>
 * </ul>
 * Como o sistema é de uso pessoal, o volume é pequeno e o cálculo é feito em memória por clareza.
 */
@Service
public class StatsService {

    private final JobOpportunityRepository jobs;
    private final JobApplicationRepository applications;
    private final InterviewRepository interviews;
    private final Clock clock;

    public StatsService(JobOpportunityRepository jobs, JobApplicationRepository applications,
                        InterviewRepository interviews, Clock clock) {
        this.jobs = jobs;
        this.applications = applications;
        this.interviews = interviews;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StatsResponse compute() {
        List<JobOpportunity> allJobs = jobs.findAllWithRequirements();
        List<JobApplication> allApplications = applications.findAllWithJob();
        Set<Long> withInterview = new HashSet<>(interviews.findApplicationIdsWithInterviews());

        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status, 0L);
        }

        long submitted = 0;
        long active = 0;
        long responded = 0;
        long interviewed = 0;
        long offers = 0;

        for (JobApplication application : allApplications) {
            ApplicationStatus status = application.getStatus();
            byStatus.merge(status, 1L, Long::sum);
            if (status.isActive()) {
                active++;
            }
            if (application.getAppliedAt() == null) {
                continue; // ainda na lista de desejos: não conta nas taxas
            }
            submitted++;
            boolean hadInterview = withInterview.contains(application.getId());
            if (hadInterview || isOneOf(status, ApplicationStatus.SCREENING, ApplicationStatus.INTERVIEWING,
                    ApplicationStatus.OFFER, ApplicationStatus.ACCEPTED, ApplicationStatus.REJECTED)) {
                responded++;
            }
            if (hadInterview || isOneOf(status, ApplicationStatus.INTERVIEWING, ApplicationStatus.OFFER,
                    ApplicationStatus.ACCEPTED)) {
                interviewed++;
            }
            if (isOneOf(status, ApplicationStatus.OFFER, ApplicationStatus.ACCEPTED)) {
                offers++;
            }
        }

        Map<WorkMode, Long> byWorkMode = new EnumMap<>(WorkMode.class);
        for (WorkMode mode : WorkMode.values()) {
            byWorkMode.put(mode, 0L);
        }
        Map<String, Long> techCounts = new HashMap<>();
        Map<String, String> techNames = new HashMap<>();
        for (JobOpportunity job : allJobs) {
            byWorkMode.merge(job.getWorkMode(), 1L, Long::sum);
            for (JobRequirement requirement : job.getRequirements()) {
                techCounts.merge(requirement.getNormalizedName(), 1L, Long::sum);
                techNames.putIfAbsent(requirement.getNormalizedName(), requirement.getName());
            }
        }
        List<TechCount> topTechnologies = techCounts.entrySet().stream()
                .map(e -> new TechCount(techNames.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingLong(TechCount::count).reversed()
                        .thenComparing(TechCount::name, String.CASE_INSENSITIVE_ORDER))
                .limit(10)
                .toList();

        LocalDateTime now = LocalDateTime.now(clock);
        long upcoming = interviews
                .findByStatusAndScheduledAtBetween(InterviewStatus.SCHEDULED, now, now.plusDays(30)).size();

        List<FunnelStep> funnel = List.of(
                new FunnelStep("Candidaturas enviadas", submitted),
                new FunnelStep("Receberam resposta", responded),
                new FunnelStep("Chegaram à entrevista", interviewed),
                new FunnelStep("Propostas", offers));

        return new StatsResponse(allJobs.size(), allApplications.size(), submitted, active, responded,
                interviewed, offers, percent(responded, submitted), percent(interviewed, submitted),
                percent(offers, submitted), upcoming, byStatus, byWorkMode, funnel, topTechnologies);
    }

    private static boolean isOneOf(ApplicationStatus status, ApplicationStatus... options) {
        for (ApplicationStatus option : options) {
            if (status == option) {
                return true;
            }
        }
        return false;
    }

    private static double percent(long part, long total) {
        if (total == 0) {
            return 0;
        }
        return Math.round(part * 1000.0 / total) / 10.0;
    }
}
