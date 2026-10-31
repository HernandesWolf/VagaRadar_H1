package com.vagaradar.web;

import com.vagaradar.domain.ApplicationStatus;
import com.vagaradar.service.ApplicationService;
import com.vagaradar.web.dto.ApplicationDtos.ApplicationDetail;
import com.vagaradar.web.dto.ApplicationDtos.ApplicationSummary;
import com.vagaradar.web.dto.ApplicationDtos.ChangeStatusRequest;
import com.vagaradar.web.dto.ApplicationDtos.CreateApplicationRequest;
import com.vagaradar.web.dto.ApplicationDtos.UpdateApplicationRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Candidaturas", description = "Acompanhamento das candidaturas e mudança de status")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista candidaturas (opcionalmente filtradas por status)")
    public List<ApplicationSummary> list(@RequestParam(required = false) ApplicationStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha a candidatura com etapas, entrevistas e contatos")
    public ApplicationDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Cria a candidatura de uma vaga (WISHLIST ou APPLIED)")
    public ResponseEntity<ApplicationDetail> create(@Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza data de candidatura, follow-up e anotações")
    public ApplicationDetail update(@PathVariable Long id, @Valid @RequestBody UpdateApplicationRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Muda o status respeitando as transições permitidas (409 se inválida)")
    public ApplicationDetail changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
        return service.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a candidatura (a vaga continua cadastrada)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
