package com.vagaradar.web.dto;

import com.vagaradar.domain.ApplicationStatus;
import com.vagaradar.domain.Contact;
import com.vagaradar.domain.Interview;
import com.vagaradar.domain.InterviewStatus;
import com.vagaradar.domain.InterviewType;
import com.vagaradar.domain.JobApplication;
import com.vagaradar.domain.SelectionStage;
import com.vagaradar.domain.StageStatus;
import com.vagaradar.domain.StageType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    // ---------- Requests ----------

    /** {@code status} aceita WISHLIST ou APPLIED (padrão: APPLIED). */
    public record CreateApplicationRequest(
            @NotNull Long jobId,
            ApplicationStatus status,
            LocalDate appliedAt,
            LocalDate followUpDate,
            @Size(max = 2000) String notes) {
    }

    public record UpdateApplicationRequest(
            LocalDate appliedAt,
            LocalDate followUpDate,
            @Size(max = 2000) String notes) {
    }

    public record ChangeStatusRequest(@NotNull ApplicationStatus status, @Size(max = 500) String note) {
    }

    public record StageRequest(
            @NotNull StageType stageType,
            @NotBlank @Size(max = 120) String title,
            StageStatus status,
            LocalDate dueDate,
            @Size(max = 1000) String notes) {
    }

    public record InterviewRequest(
            @NotNull InterviewType interviewType,
            @NotNull LocalDateTime scheduledAt,
            @Size(max = 120) String interviewer,
            @Size(max = 255) String location,
            InterviewStatus status,
            @Size(max = 2000) String feedback) {
    }

    public record ContactRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 120) String jobRole,
            @Email @Size(max = 160) String email,
            @Size(max = 40) String phone,
            @Size(max = 255) String linkedinUrl,
            @Size(max = 1000) String notes) {
    }

    // ---------- Responses ----------

    public record StageResponse(Long id, StageType stageType, String title, StageStatus status,
                                LocalDate dueDate, String notes, int sortOrder) {
        public static StageResponse from(SelectionStage s) {
            return new StageResponse(s.getId(), s.getStageType(), s.getTitle(), s.getStatus(),
                    s.getDueDate(), s.getNotes(), s.getSortOrder());
        }
    }

    public record InterviewResponse(Long id, InterviewType interviewType, LocalDateTime scheduledAt,
                                    String interviewer, String location, InterviewStatus status,
                                    String feedback) {
        public static InterviewResponse from(Interview i) {
            return new InterviewResponse(i.getId(), i.getInterviewType(), i.getScheduledAt(),
                    i.getInterviewer(), i.getLocation(), i.getStatus(), i.getFeedback());
        }
    }

    public record ContactResponse(Long id, String name, String jobRole, String email, String phone,
                                  String linkedinUrl, String notes) {
        public static ContactResponse from(Contact c) {
            return new ContactResponse(c.getId(), c.getName(), c.getJobRole(), c.getEmail(), c.getPhone(),
                    c.getLinkedinUrl(), c.getNotes());
        }
    }

    public record ApplicationSummary(
            Long id, Long jobId, String jobTitle, String company, ApplicationStatus status,
            LocalDate appliedAt, LocalDate followUpDate, LocalDateTime statusChangedAt,
            List<ApplicationStatus> allowedTransitions,
            int stageCount, int interviewCount, int contactCount) {

        public static ApplicationSummary from(JobApplication a) {
            return new ApplicationSummary(a.getId(), a.getJob().getId(), a.getJob().getTitle(),
                    a.getJob().getCompany(), a.getStatus(), a.getAppliedAt(), a.getFollowUpDate(),
                    a.getStatusChangedAt(), new ArrayList<>(a.getStatus().allowedTransitions()),
                    a.getStages().size(), a.getInterviews().size(), a.getContacts().size());
        }
    }

    public record ApplicationDetail(
            Long id, Long jobId, String jobTitle, String company, ApplicationStatus status,
            LocalDate appliedAt, LocalDate followUpDate, LocalDateTime statusChangedAt, String notes,
            List<ApplicationStatus> allowedTransitions,
            List<StageResponse> stages, List<InterviewResponse> interviews, List<ContactResponse> contacts) {

        public static ApplicationDetail from(JobApplication a) {
            return new ApplicationDetail(a.getId(), a.getJob().getId(), a.getJob().getTitle(),
                    a.getJob().getCompany(), a.getStatus(), a.getAppliedAt(), a.getFollowUpDate(),
                    a.getStatusChangedAt(), a.getNotes(), new ArrayList<>(a.getStatus().allowedTransitions()),
                    a.getStages().stream().map(StageResponse::from).toList(),
                    a.getInterviews().stream().map(InterviewResponse::from).toList(),
                    a.getContacts().stream().map(ContactResponse::from).toList());
        }
    }
}
