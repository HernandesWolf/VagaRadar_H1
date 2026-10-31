package com.vagaradar.service;

import com.vagaradar.analysis.SkillCatalog;
import com.vagaradar.domain.ApplicationStatus;
import com.vagaradar.domain.JobApplication;
import com.vagaradar.domain.JobOpportunity;
import com.vagaradar.domain.JobRequirement;
import com.vagaradar.domain.SkillLevel;
import com.vagaradar.repository.JobApplicationRepository;
import com.vagaradar.repository.JobOpportunityRepository;
import com.vagaradar.repository.JobSpecifications;
import com.vagaradar.web.dto.JobDtos.JobDetail;
import com.vagaradar.web.dto.JobDtos.JobRequest;
import com.vagaradar.web.dto.JobDtos.JobSummary;
import com.vagaradar.web.dto.JobDtos.RequirementRequest;
import com.vagaradar.web.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JobService {

    /** Campos pelos quais a listagem pode ser ordenada (whitelist: evita expor nomes internos). */
    private static final Map<String, String> SORTABLE = Map.of(
            "createdAt", "createdAt",
            "deadline", "deadline",
            "company", "company",
            "title", "title",
            "salary", "salaryMax");

    private final JobOpportunityRepository jobs;
    private final JobApplicationRepository applications;

    public JobService(JobOpportunityRepository jobs, JobApplicationRepository applications) {
        this.jobs = jobs;
        this.applications = applications;
    }

    @Transactional(readOnly = true)
    public PageResponse<JobSummary> search(JobFilter filter, String sortBy, String direction, int page, int size) {
        String property = SORTABLE.get(sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy);
        if (property == null) {
            throw new IllegalArgumentException("Campo de ordenação inválido: '" + sortBy
                    + "'. Use um destes: " + SORTABLE.keySet());
        }
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(dir, property).and(Sort.by(Sort.Direction.DESC, "id")));

        Page<JobOpportunity> result = jobs.findAll(JobSpecifications.from(filter), pageable);

        List<Long> ids = result.getContent().stream().map(JobOpportunity::getId).toList();
        Map<Long, ApplicationStatus> statusByJob = new HashMap<>();
        if (!ids.isEmpty()) {
            applications.findByJobIdIn(ids).forEach(a -> statusByJob.put(a.getJob().getId(), a.getStatus()));
        }
        List<JobSummary> content = result.getContent().stream()
                .map(job -> JobSummary.from(job, statusByJob.get(job.getId())))
                .toList();
        return PageResponse.of(result, content);
    }

    @Transactional(readOnly = true)
    public JobDetail get(Long id) {
        return toDetail(find(id));
    }

    @Transactional
    public JobDetail create(JobRequest request) {
        validate(request);
        JobOpportunity job = new JobOpportunity(request.title().trim(), request.company().trim(), request.workMode());
        copyFields(job, request);
        syncRequirements(job, request.requirements());
        jobs.save(job);
        return toDetail(job);
    }

    @Transactional
    public JobDetail update(Long id, JobRequest request) {
        validate(request);
        JobOpportunity job = find(id);
        job.setTitle(request.title().trim());
        job.setCompany(request.company().trim());
        job.setWorkMode(request.workMode());
        copyFields(job, request);
        syncRequirements(job, request.requirements());
        return toDetail(job);
    }

    @Transactional
    public void delete(Long id) {
        JobOpportunity job = find(id);
        applications.findByJobId(id).ifPresent(applications::delete);
        jobs.delete(job);
    }

    // ---------- helpers ----------

    private JobOpportunity find(Long id) {
        return jobs.findById(id).orElseThrow(() -> new NotFoundException("Vaga não encontrada: " + id));
    }

    private JobDetail toDetail(JobOpportunity job) {
        Optional<JobApplication> application = job.getId() == null
                ? Optional.empty() : applications.findByJobId(job.getId());
        return JobDetail.from(job,
                application.map(JobApplication::getId).orElse(null),
                application.map(JobApplication::getStatus).orElse(null));
    }

    private static void validate(JobRequest r) {
        if (r.salaryMin() != null && r.salaryMax() != null && r.salaryMax().compareTo(r.salaryMin()) < 0) {
            throw new IllegalArgumentException("O salário máximo não pode ser menor que o mínimo.");
        }
    }

    private static void copyFields(JobOpportunity job, JobRequest r) {
        job.setLocation(blankToNull(r.location()));
        job.setSalaryMin(r.salaryMin());
        job.setSalaryMax(r.salaryMax());
        job.setSourceUrl(blankToNull(r.sourceUrl()));
        job.setDescription(blankToNull(r.description()));
        job.setDeadline(r.deadline());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Sincroniza os requisitos por diferença (atualiza os existentes, adiciona os novos, remove os ausentes).
     * Evita apagar e recriar linhas, o que violaria a unicidade (vaga, requisito) durante o flush.
     */
    private static void syncRequirements(JobOpportunity job, List<RequirementRequest> incoming) {
        Map<String, RequirementRequest> wanted = new LinkedHashMap<>();
        if (incoming != null) {
            for (RequirementRequest request : incoming) {
                String key = SkillCatalog.key(request.name());
                if (!key.isBlank()) {
                    wanted.merge(key, request, JobService::stronger);
                }
            }
        }

        job.getRequirements().removeIf(existing -> !wanted.containsKey(existing.getNormalizedName()));
        Map<String, JobRequirement> existingByKey = job.getRequirements().stream()
                .collect(Collectors.toMap(JobRequirement::getNormalizedName, r -> r));

        wanted.forEach((key, request) -> {
            boolean mandatory = request.mandatory() == null || request.mandatory();
            SkillLevel level = request.minLevel() == null ? SkillLevel.BEGINNER : request.minLevel();
            JobRequirement existing = existingByKey.get(key);
            if (existing == null) {
                job.getRequirements().add(new JobRequirement(job, SkillCatalog.canonicalName(request.name()),
                        key, mandatory, level));
            } else {
                existing.setName(SkillCatalog.canonicalName(request.name()));
                existing.setMandatory(mandatory);
                existing.setMinLevel(level);
            }
        });
    }

    /** Se o mesmo requisito vier duas vezes na requisição, prevalece a exigência mais forte. */
    private static RequirementRequest stronger(RequirementRequest a, RequirementRequest b) {
        boolean mandatory = (a.mandatory() == null || a.mandatory()) || (b.mandatory() == null || b.mandatory());
        SkillLevel levelA = a.minLevel() == null ? SkillLevel.BEGINNER : a.minLevel();
        SkillLevel levelB = b.minLevel() == null ? SkillLevel.BEGINNER : b.minLevel();
        return new RequirementRequest(a.name(), mandatory, levelA.rank() >= levelB.rank() ? levelA : levelB);
    }
}
