package com.vagaradar.web;

import com.vagaradar.analysis.RequirementExtractor.Suggestion;
import com.vagaradar.domain.WorkMode;
import com.vagaradar.service.GapService;
import com.vagaradar.service.JobFilter;
import com.vagaradar.service.JobService;
import com.vagaradar.web.dto.GapDtos.JobGapResponse;
import com.vagaradar.web.dto.JobDtos.ExtractRequest;
import com.vagaradar.web.dto.JobDtos.JobDetail;
import com.vagaradar.web.dto.JobDtos.JobRequest;
import com.vagaradar.web.dto.JobDtos.JobSummary;
import com.vagaradar.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Vagas", description = "Cadastro, busca com filtros e análise de aderência das oportunidades")
public class JobController {

    private final JobService jobService;
    private final GapService gapService;

    public JobController(JobService jobService, GapService gapService) {
        this.jobService = jobService;
        this.gapService = gapService;
    }

    @GetMapping
    @Operation(summary = "Lista vagas com filtros (texto, tecnologia, localidade, modalidade, salário) e paginação")
    public PageResponse<JobSummary> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> technology,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return jobService.search(new JobFilter(q, technology, location, workMode, minSalary),
                sortBy, direction, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma vaga, com requisitos e situação da candidatura")
    public JobDetail get(@PathVariable Long id) {
        return jobService.get(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra uma vaga (com seus requisitos)")
    public ResponseEntity<JobDetail> create(@Valid @RequestBody JobRequest request) {
        JobDetail created = jobService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma vaga e sincroniza seus requisitos")
    public JobDetail update(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        return jobService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a vaga e a candidatura associada")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/gap-analysis")
    @Operation(summary = "Compara os requisitos da vaga com suas competências e aponta as lacunas")
    public JobGapResponse gapAnalysis(@PathVariable Long id) {
        return gapService.analyzeJob(id);
    }

    @PostMapping("/extract-requirements")
    @Operation(summary = "Sugere requisitos a partir do texto da vaga (regras determinísticas, para revisão)")
    public List<Suggestion> extractRequirements(@Valid @RequestBody ExtractRequest request) {
        return gapService.extractRequirements(request.description());
    }
}
