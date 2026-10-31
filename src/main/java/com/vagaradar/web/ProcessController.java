package com.vagaradar.web;

import com.vagaradar.service.ProcessService;
import com.vagaradar.web.dto.ApplicationDtos.ContactRequest;
import com.vagaradar.web.dto.ApplicationDtos.ContactResponse;
import com.vagaradar.web.dto.ApplicationDtos.InterviewRequest;
import com.vagaradar.web.dto.ApplicationDtos.InterviewResponse;
import com.vagaradar.web.dto.ApplicationDtos.StageRequest;
import com.vagaradar.web.dto.ApplicationDtos.StageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications/{applicationId}")
@Tag(name = "Processo seletivo", description = "Etapas, entrevistas e contatos de uma candidatura")
public class ProcessController {

    private final ProcessService service;

    public ProcessController(ProcessService service) {
        this.service = service;
    }

    // ----- Etapas -----

    @GetMapping("/stages")
    @Operation(summary = "Lista as etapas do processo seletivo")
    public List<StageResponse> listStages(@PathVariable Long applicationId) {
        return service.listStages(applicationId);
    }

    @PostMapping("/stages")
    @Operation(summary = "Adiciona uma etapa")
    public ResponseEntity<StageResponse> addStage(@PathVariable Long applicationId,
                                                  @Valid @RequestBody StageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addStage(applicationId, request));
    }

    @PutMapping("/stages/{stageId}")
    @Operation(summary = "Atualiza uma etapa")
    public StageResponse updateStage(@PathVariable Long applicationId, @PathVariable Long stageId,
                                     @Valid @RequestBody StageRequest request) {
        return service.updateStage(applicationId, stageId, request);
    }

    @DeleteMapping("/stages/{stageId}")
    @Operation(summary = "Remove uma etapa")
    public ResponseEntity<Void> deleteStage(@PathVariable Long applicationId, @PathVariable Long stageId) {
        service.deleteStage(applicationId, stageId);
        return ResponseEntity.noContent().build();
    }

    // ----- Entrevistas -----

    @GetMapping("/interviews")
    @Operation(summary = "Lista as entrevistas")
    public List<InterviewResponse> listInterviews(@PathVariable Long applicationId) {
        return service.listInterviews(applicationId);
    }

    @PostMapping("/interviews")
    @Operation(summary = "Registra uma entrevista")
    public ResponseEntity<InterviewResponse> addInterview(@PathVariable Long applicationId,
                                                          @Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addInterview(applicationId, request));
    }

    @PutMapping("/interviews/{interviewId}")
    @Operation(summary = "Atualiza uma entrevista (inclusive o feedback após realizá-la)")
    public InterviewResponse updateInterview(@PathVariable Long applicationId, @PathVariable Long interviewId,
                                             @Valid @RequestBody InterviewRequest request) {
        return service.updateInterview(applicationId, interviewId, request);
    }

    @DeleteMapping("/interviews/{interviewId}")
    @Operation(summary = "Remove uma entrevista")
    public ResponseEntity<Void> deleteInterview(@PathVariable Long applicationId, @PathVariable Long interviewId) {
        service.deleteInterview(applicationId, interviewId);
        return ResponseEntity.noContent().build();
    }

    // ----- Contatos -----

    @GetMapping("/contacts")
    @Operation(summary = "Lista os contatos")
    public List<ContactResponse> listContacts(@PathVariable Long applicationId) {
        return service.listContacts(applicationId);
    }

    @PostMapping("/contacts")
    @Operation(summary = "Adiciona um contato (recrutador, gestor...)")
    public ResponseEntity<ContactResponse> addContact(@PathVariable Long applicationId,
                                                      @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addContact(applicationId, request));
    }

    @PutMapping("/contacts/{contactId}")
    @Operation(summary = "Atualiza um contato")
    public ContactResponse updateContact(@PathVariable Long applicationId, @PathVariable Long contactId,
                                         @Valid @RequestBody ContactRequest request) {
        return service.updateContact(applicationId, contactId, request);
    }

    @DeleteMapping("/contacts/{contactId}")
    @Operation(summary = "Remove um contato")
    public ResponseEntity<Void> deleteContact(@PathVariable Long applicationId, @PathVariable Long contactId) {
        service.deleteContact(applicationId, contactId);
        return ResponseEntity.noContent().build();
    }
}
