package com.vagaradar.web.dto;

import com.vagaradar.domain.ApplicationStatus;
import com.vagaradar.domain.JobOpportunity;
import com.vagaradar.domain.JobRequirement;
import com.vagaradar.domain.SkillLevel;
import com.vagaradar.domain.WorkMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class JobDtos {

    private JobDtos() {
    }

    /** {@code mandatory} nulo = obrigatório; {@code minLevel} nulo = BEGINNER. */
    public record RequirementRequest(@NotBlank @Size(max = 80) String name, Boolean mandatory, SkillLevel minLevel) {
    }

    public record JobRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 120) String company,
            @Size(max = 120) String location,
            @NotNull WorkMode workMode,
            @PositiveOrZero BigDecimal salaryMin,
            @PositiveOrZero BigDecimal salaryMax,
            @Size(max = 500) String sourceUrl,
            @Size(max = 5000) String description,
            LocalDate deadline,
            @Valid List<RequirementRequest> requirements) {
    }

    public record ExtractRequest(@NotBlank @Size(max = 5000) String description) {
    }

    public record RequirementResponse(Long id, String name, boolean mandatory, SkillLevel minLevel) {
        public static RequirementResponse from(JobRequirement r) {
            return new RequirementResponse(r.getId(), r.getName(), r.isMandatory(), r.getMinLevel());
        }
    }

    public record JobSummary(
            Long id, String title, String company, String location, WorkMode workMode,
            BigDecimal salaryMin, BigDecimal salaryMax, LocalDate deadline,
            List<String> technologies, ApplicationStatus applicationStatus, LocalDateTime createdAt) {

        public static JobSummary from(JobOpportunity job, ApplicationStatus applicationStatus) {
            List<String> technologies = job.getRequirements().stream()
                    .map(JobRequirement::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
            return new JobSummary(job.getId(), job.getTitle(), job.getCompany(), job.getLocation(),
                    job.getWorkMode(), job.getSalaryMin(), job.getSalaryMax(), job.getDeadline(),
                    technologies, applicationStatus, job.getCreatedAt());
        }
    }

    public record JobDetail(
            Long id, String title, String company, String location, WorkMode workMode,
            BigDecimal salaryMin, BigDecimal salaryMax, String sourceUrl, String description,
            LocalDate deadline, List<RequirementResponse> requirements,
            Long applicationId, ApplicationStatus applicationStatus, LocalDateTime createdAt) {

        public static JobDetail from(JobOpportunity job, Long applicationId, ApplicationStatus applicationStatus) {
            List<RequirementResponse> requirements = job.getRequirements().stream()
                    .sorted((a, b) -> {
                        if (a.isMandatory() != b.isMandatory()) {
                            return a.isMandatory() ? -1 : 1;
                        }
                        return a.getName().compareToIgnoreCase(b.getName());
                    })
                    .map(RequirementResponse::from).toList();
            return new JobDetail(job.getId(), job.getTitle(), job.getCompany(), job.getLocation(),
                    job.getWorkMode(), job.getSalaryMin(), job.getSalaryMax(), job.getSourceUrl(),
                    job.getDescription(), job.getDeadline(), requirements,
                    applicationId, applicationStatus, job.getCreatedAt());
        }
    }
}
