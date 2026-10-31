package com.vagaradar.service;

import com.vagaradar.domain.ApplicationStatus;
import com.vagaradar.domain.JobApplication;
import com.vagaradar.domain.JobOpportunity;
import com.vagaradar.repository.JobApplicationRepository;
import com.vagaradar.repository.JobOpportunityRepository;
import com.vagaradar.web.dto.ApplicationDtos.ApplicationDetail;
import com.vagaradar.web.dto.ApplicationDtos.ApplicationSummary;
import com.vagaradar.web.dto.ApplicationDtos.ChangeStatusRequest;
import com.vagaradar.web.dto.ApplicationDtos.CreateApplicationRequest;
import com.vagaradar.web.dto.ApplicationDtos.UpdateApplicationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ApplicationService {

    private static final int MAX_NOTES = 2000;
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

    private final JobApplicationRepository applications;
    private final JobOpportunityRepository jobs;
    private final Clock clock;

    public ApplicationService(JobApplicationRepository applications, JobOpportunityRepository jobs, Clock clock) {
        this.applications = applications;
        this.jobs = jobs;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ApplicationSummary> list(ApplicationStatus status) {
        List<JobApplication> result = status == null
                ? applications.findAllWithJob()
                : applications.findByStatusWithJob(status);
        return result.stream().map(ApplicationSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public ApplicationDetail get(Long id) {
        return ApplicationDetail.from(find(id));
    }

    @Transactional
    public ApplicationDetail create(CreateApplicationRequest request) {
        JobOpportunity job = jobs.findById(request.jobId())
                .orElseThrow(() -> new NotFoundException("Vaga não encontrada: " + request.jobId()));
        if (applications.existsByJobId(job.getId())) {
            throw new BusinessRuleException("Já existe uma candidatura para esta vaga.");
        }

        ApplicationStatus status = request.status() == null ? ApplicationStatus.APPLIED : request.status();
        if (status != ApplicationStatus.WISHLIST && status != ApplicationStatus.APPLIED) {
            throw new IllegalArgumentException(
                    "Uma nova candidatura só pode começar como WISHLIST ou APPLIED. Depois, avance o status.");
        }

        LocalDate today = LocalDate.now(clock);
        JobApplication application = new JobApplication(job, status, LocalDateTime.now(clock));
        if (status == ApplicationStatus.APPLIED) {
            LocalDate appliedAt = request.appliedAt() == null ? today : request.appliedAt();
            if (appliedAt.isAfter(today)) {
                throw new IllegalArgumentException("A data da candidatura não pode estar no futuro.");
            }
            application.setAppliedAt(appliedAt);
        }
        application.setFollowUpDate(request.followUpDate());
        application.setNotes(blankToNull(request.notes()));
        applications.save(application);
        return ApplicationDetail.from(application);
    }

    @Transactional
    public ApplicationDetail update(Long id, UpdateApplicationRequest request) {
        JobApplication application = find(id);
        if (request.appliedAt() != null && request.appliedAt().isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("A data da candidatura não pode estar no futuro.");
        }
        application.setAppliedAt(request.appliedAt() != null ? request.appliedAt() : application.getAppliedAt());
        application.setFollowUpDate(request.followUpDate());
        application.setNotes(blankToNull(request.notes()));
        return ApplicationDetail.from(application);
    }

    /** Move a candidatura na máquina de estados; transições incoerentes geram HTTP 409. */
    @Transactional
    public ApplicationDetail changeStatus(Long id, ChangeStatusRequest request) {
        JobApplication application = find(id);
        ApplicationStatus from = application.getStatus();
        ApplicationStatus to = request.status();
        if (!from.canTransitionTo(to)) {
            throw new InvalidStatusTransitionException(from, to);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        application.setStatus(to);
        application.setStatusChangedAt(now);
        if (to == ApplicationStatus.APPLIED && application.getAppliedAt() == null) {
            application.setAppliedAt(now.toLocalDate());
        }
        if (request.note() != null && !request.note().isBlank()) {
            String entry = "[" + DAY_MONTH.format(now) + " " + from + "→" + to + "] " + request.note().trim();
            String current = application.getNotes();
            String merged = current == null || current.isBlank() ? entry : current + "\n" + entry;
            if (merged.length() > MAX_NOTES) {
                merged = merged.substring(merged.length() - MAX_NOTES);
            }
            application.setNotes(merged);
        }
        return ApplicationDetail.from(application);
    }

    @Transactional
    public void delete(Long id) {
        applications.delete(find(id));
    }

    private JobApplication find(Long id) {
        return applications.findById(id)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada: " + id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
