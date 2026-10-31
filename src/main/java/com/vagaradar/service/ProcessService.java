package com.vagaradar.service;

import com.vagaradar.domain.Contact;
import com.vagaradar.domain.Interview;
import com.vagaradar.domain.InterviewStatus;
import com.vagaradar.domain.JobApplication;
import com.vagaradar.domain.SelectionStage;
import com.vagaradar.domain.StageStatus;
import com.vagaradar.repository.ContactRepository;
import com.vagaradar.repository.InterviewRepository;
import com.vagaradar.repository.JobApplicationRepository;
import com.vagaradar.repository.SelectionStageRepository;
import com.vagaradar.web.dto.ApplicationDtos.ContactRequest;
import com.vagaradar.web.dto.ApplicationDtos.ContactResponse;
import com.vagaradar.web.dto.ApplicationDtos.InterviewRequest;
import com.vagaradar.web.dto.ApplicationDtos.InterviewResponse;
import com.vagaradar.web.dto.ApplicationDtos.StageRequest;
import com.vagaradar.web.dto.ApplicationDtos.StageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

/** Etapas do processo seletivo, entrevistas e contatos de uma candidatura. */
@Service
public class ProcessService {

    private final JobApplicationRepository applications;
    private final SelectionStageRepository stageRepository;
    private final InterviewRepository interviewRepository;
    private final ContactRepository contactRepository;

    public ProcessService(JobApplicationRepository applications, SelectionStageRepository stageRepository,
                          InterviewRepository interviewRepository, ContactRepository contactRepository) {
        this.applications = applications;
        this.stageRepository = stageRepository;
        this.interviewRepository = interviewRepository;
        this.contactRepository = contactRepository;
    }

    // ---------- Etapas ----------

    @Transactional(readOnly = true)
    public List<StageResponse> listStages(Long applicationId) {
        return find(applicationId).getStages().stream().map(StageResponse::from).toList();
    }

    @Transactional
    public StageResponse addStage(Long applicationId, StageRequest request) {
        JobApplication application = find(applicationId);
        SelectionStage stage = new SelectionStage(application);
        applyStage(stage, request);
        stage.setSortOrder(application.getStages().size() + 1);
        application.getStages().add(stage);
        stageRepository.save(stage);
        return StageResponse.from(stage);
    }

    @Transactional
    public StageResponse updateStage(Long applicationId, Long stageId, StageRequest request) {
        SelectionStage stage = child(find(applicationId).getStages(), stageId, SelectionStage::getId, "Etapa");
        applyStage(stage, request);
        return StageResponse.from(stage);
    }

    @Transactional
    public void deleteStage(Long applicationId, Long stageId) {
        JobApplication application = find(applicationId);
        SelectionStage stage = child(application.getStages(), stageId, SelectionStage::getId, "Etapa");
        application.getStages().remove(stage);
    }

    private static void applyStage(SelectionStage stage, StageRequest request) {
        stage.setStageType(request.stageType());
        stage.setTitle(request.title().trim());
        stage.setStatus(request.status() == null ? StageStatus.PENDING : request.status());
        stage.setDueDate(request.dueDate());
        stage.setNotes(blankToNull(request.notes()));
    }

    // ---------- Entrevistas ----------

    @Transactional(readOnly = true)
    public List<InterviewResponse> listInterviews(Long applicationId) {
        return find(applicationId).getInterviews().stream().map(InterviewResponse::from).toList();
    }

    @Transactional
    public InterviewResponse addInterview(Long applicationId, InterviewRequest request) {
        JobApplication application = find(applicationId);
        Interview interview = new Interview(application);
        applyInterview(interview, request);
        application.getInterviews().add(interview);
        interviewRepository.save(interview);
        return InterviewResponse.from(interview);
    }

    @Transactional
    public InterviewResponse updateInterview(Long applicationId, Long interviewId, InterviewRequest request) {
        Interview interview = child(find(applicationId).getInterviews(), interviewId, Interview::getId, "Entrevista");
        applyInterview(interview, request);
        return InterviewResponse.from(interview);
    }

    @Transactional
    public void deleteInterview(Long applicationId, Long interviewId) {
        JobApplication application = find(applicationId);
        Interview interview = child(application.getInterviews(), interviewId, Interview::getId, "Entrevista");
        application.getInterviews().remove(interview);
    }

    private static void applyInterview(Interview interview, InterviewRequest request) {
        interview.setInterviewType(request.interviewType());
        interview.setScheduledAt(request.scheduledAt());
        interview.setInterviewer(blankToNull(request.interviewer()));
        interview.setLocation(blankToNull(request.location()));
        interview.setStatus(request.status() == null ? InterviewStatus.SCHEDULED : request.status());
        interview.setFeedback(blankToNull(request.feedback()));
    }

    // ---------- Contatos ----------

    @Transactional(readOnly = true)
    public List<ContactResponse> listContacts(Long applicationId) {
        return find(applicationId).getContacts().stream().map(ContactResponse::from).toList();
    }

    @Transactional
    public ContactResponse addContact(Long applicationId, ContactRequest request) {
        JobApplication application = find(applicationId);
        Contact contact = new Contact(application);
        applyContact(contact, request);
        application.getContacts().add(contact);
        contactRepository.save(contact);
        return ContactResponse.from(contact);
    }

    @Transactional
    public ContactResponse updateContact(Long applicationId, Long contactId, ContactRequest request) {
        Contact contact = child(find(applicationId).getContacts(), contactId, Contact::getId, "Contato");
        applyContact(contact, request);
        return ContactResponse.from(contact);
    }

    @Transactional
    public void deleteContact(Long applicationId, Long contactId) {
        JobApplication application = find(applicationId);
        Contact contact = child(application.getContacts(), contactId, Contact::getId, "Contato");
        application.getContacts().remove(contact);
    }

    private static void applyContact(Contact contact, ContactRequest request) {
        contact.setName(request.name().trim());
        contact.setJobRole(blankToNull(request.jobRole()));
        contact.setEmail(blankToNull(request.email()));
        contact.setPhone(blankToNull(request.phone()));
        contact.setLinkedinUrl(blankToNull(request.linkedinUrl()));
        contact.setNotes(blankToNull(request.notes()));
    }

    // ---------- helpers ----------

    private JobApplication find(Long id) {
        return applications.findById(id)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada: " + id));
    }

    /** Localiza o filho dentro da própria candidatura: garante que ele realmente pertence a ela. */
    private static <T> T child(List<T> items, Long id, Function<T, Long> idOf, String label) {
        return items.stream()
                .filter(item -> id.equals(idOf.apply(item)))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(label + " não encontrada(o) nesta candidatura: " + id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
