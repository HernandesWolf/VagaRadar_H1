package com.vagaradar.service;

import com.vagaradar.domain.ApplicationStatus;
import com.vagaradar.domain.Interview;
import com.vagaradar.domain.InterviewStatus;
import com.vagaradar.domain.JobApplication;
import com.vagaradar.domain.JobOpportunity;
import com.vagaradar.domain.SelectionStage;
import com.vagaradar.domain.StageStatus;
import com.vagaradar.repository.InterviewRepository;
import com.vagaradar.repository.JobApplicationRepository;
import com.vagaradar.repository.JobOpportunityRepository;
import com.vagaradar.repository.SelectionStageRepository;
import com.vagaradar.web.dto.Alert;
import com.vagaradar.web.dto.Alert.Severity;
import com.vagaradar.web.dto.Alert.Type;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gera alertas sob demanda (sem agendador): prazos de vagas, entrevistas próximas,
 * follow-ups vencidos, etapas com prazo e candidaturas sem resposta há muito tempo.
 */
@Service
public class AlertService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final JobOpportunityRepository jobs;
    private final JobApplicationRepository applications;
    private final InterviewRepository interviews;
    private final SelectionStageRepository stages;
    private final Clock clock;
    private final int defaultWindowDays;
    private final int staleDays;

    public AlertService(JobOpportunityRepository jobs, JobApplicationRepository applications,
                        InterviewRepository interviews, SelectionStageRepository stages, Clock clock,
                        @Value("${vagaradar.alerts.default-window-days:7}") int defaultWindowDays,
                        @Value("${vagaradar.alerts.stale-days:14}") int staleDays) {
        this.jobs = jobs;
        this.applications = applications;
        this.interviews = interviews;
        this.stages = stages;
        this.clock = clock;
        this.defaultWindowDays = defaultWindowDays;
        this.staleDays = staleDays;
    }

    @Transactional(readOnly = true)
    public List<Alert> compute(Integer windowDays) {
        int window = windowDays == null ? defaultWindowDays : Math.min(Math.max(windowDays, 1), 90);
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Alert> alerts = new ArrayList<>();

        // 1) Prazo de vagas em que ainda não me candidatei
        List<JobOpportunity> closing = jobs.findByDeadlineBetween(today, today.plusDays(window));
        Map<Long, JobApplication> applicationByJob = new HashMap<>();
        if (!closing.isEmpty()) {
            applications.findByJobIdIn(closing.stream().map(JobOpportunity::getId).toList())
                    .forEach(a -> applicationByJob.put(a.getJob().getId(), a));
        }
        for (JobOpportunity job : closing) {
            JobApplication application = applicationByJob.get(job.getId());
            boolean alreadyApplied = application != null && application.getAppliedAt() != null;
            if (alreadyApplied) {
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(today, job.getDeadline());
            alerts.add(new Alert(Type.JOB_DEADLINE, severityByDays(daysLeft),
                    "Prazo para se candidatar a \"" + job.getTitle() + "\" (" + job.getCompany() + ") "
                            + when(daysLeft) + " — " + DATE.format(job.getDeadline()),
                    job.getDeadline(), job.getId(), application == null ? null : application.getId(), daysLeft));
        }

        // 2) Entrevistas agendadas
        for (Interview interview : interviews.findByStatusAndScheduledAtBetween(
                InterviewStatus.SCHEDULED, now, now.plusDays(window))) {
            JobApplication application = interview.getApplication();
            long hours = Duration.between(now, interview.getScheduledAt()).toHours();
            long daysLeft = ChronoUnit.DAYS.between(today, interview.getScheduledAt().toLocalDate());
            Severity severity = hours <= 24 ? Severity.CRITICAL : (daysLeft <= 3 ? Severity.WARNING : Severity.INFO);
            alerts.add(new Alert(Type.INTERVIEW_UPCOMING, severity,
                    "Entrevista \"" + application.getJob().getTitle() + "\" (" + application.getJob().getCompany()
                            + ") em " + DATE_TIME.format(interview.getScheduledAt()),
                    interview.getScheduledAt().toLocalDate(), application.getJob().getId(),
                    application.getId(), daysLeft));
        }

        // 3) Follow-ups vencidos ou chegando
        for (JobApplication application : applications.findByFollowUpDateLessThanEqual(today.plusDays(window))) {
            if (!application.getStatus().isActive()) {
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(today, application.getFollowUpDate());
            Severity severity = daysLeft < 0 ? Severity.CRITICAL : severityByDays(daysLeft);
            String text = daysLeft < 0
                    ? "Follow-up atrasado há " + (-daysLeft) + " dia(s)"
                    : "Follow-up " + when(daysLeft);
            alerts.add(new Alert(Type.FOLLOW_UP_DUE, severity,
                    text + " — \"" + application.getJob().getTitle() + "\" (" + application.getJob().getCompany() + ")",
                    application.getFollowUpDate(), application.getJob().getId(), application.getId(), daysLeft));
        }

        // 4) Etapas do processo com prazo
        Set<StageStatus> open = EnumSet.of(StageStatus.PENDING, StageStatus.IN_PROGRESS);
        for (SelectionStage stage : stages.findByStatusInAndDueDateLessThanEqual(open, today.plusDays(window))) {
            JobApplication application = stage.getApplication();
            if (application.getStatus().isTerminal()) {
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(today, stage.getDueDate());
            Severity severity = daysLeft < 0 ? Severity.CRITICAL : severityByDays(daysLeft);
            String text = daysLeft < 0
                    ? "Etapa atrasada há " + (-daysLeft) + " dia(s)"
                    : "Etapa com prazo " + when(daysLeft);
            alerts.add(new Alert(Type.STAGE_DUE, severity,
                    text + ": " + stage.getTitle() + " — \"" + application.getJob().getTitle() + "\" ("
                            + application.getJob().getCompany() + ")",
                    stage.getDueDate(), application.getJob().getId(), application.getId(), daysLeft));
        }

        // 5) Candidaturas sem resposta há muito tempo e sem follow-up definido
        for (JobApplication application : applications.findAllWithJob()) {
            if (application.getStatus() != ApplicationStatus.APPLIED || application.getFollowUpDate() != null) {
                continue;
            }
            long waiting = ChronoUnit.DAYS.between(application.getStatusChangedAt().toLocalDate(), today);
            if (waiting >= staleDays) {
                alerts.add(new Alert(Type.STALE_APPLICATION, Severity.INFO,
                        "Sem resposta há " + waiting + " dias — considere enviar um follow-up para \""
                                + application.getJob().getTitle() + "\" (" + application.getJob().getCompany() + ")",
                        today, application.getJob().getId(), application.getId(), -waiting));
            }
        }

        alerts.sort(Comparator.comparing(Alert::severity).thenComparing(Alert::date));
        return alerts;
    }

    private static Severity severityByDays(long daysLeft) {
        if (daysLeft <= 1) {
            return Severity.CRITICAL;
        }
        return daysLeft <= 3 ? Severity.WARNING : Severity.INFO;
    }

    private static String when(long daysLeft) {
        if (daysLeft <= 0) {
            return "é hoje";
        }
        return daysLeft == 1 ? "é amanhã" : "em " + daysLeft + " dias";
    }
}
