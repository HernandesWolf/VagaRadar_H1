package com.vagaradar.service;

import com.vagaradar.analysis.GapAnalyzer;
import com.vagaradar.analysis.GapAnalyzer.GapPriority;
import com.vagaradar.analysis.GapAnalyzer.GapReport;
import com.vagaradar.analysis.GapAnalyzer.RequirementSpec;
import com.vagaradar.analysis.GapAnalyzer.SkillSpec;
import com.vagaradar.analysis.RequirementExtractor;
import com.vagaradar.analysis.RequirementExtractor.Suggestion;
import com.vagaradar.domain.ApplicationStatus;
import com.vagaradar.domain.JobApplication;
import com.vagaradar.domain.JobOpportunity;
import com.vagaradar.repository.JobApplicationRepository;
import com.vagaradar.repository.JobOpportunityRepository;
import com.vagaradar.repository.UserSkillRepository;
import com.vagaradar.web.dto.GapDtos.JobGapResponse;
import com.vagaradar.web.dto.GapDtos.RankingItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cola entre o banco de dados e o motor puro de análise de lacunas. */
@Service
public class GapService {

    private final GapAnalyzer analyzer;
    private final RequirementExtractor extractor;
    private final JobOpportunityRepository jobs;
    private final JobApplicationRepository applications;
    private final UserSkillRepository skills;

    public GapService(GapAnalyzer analyzer, RequirementExtractor extractor, JobOpportunityRepository jobs,
                      JobApplicationRepository applications, UserSkillRepository skills) {
        this.analyzer = analyzer;
        this.extractor = extractor;
        this.jobs = jobs;
        this.applications = applications;
        this.skills = skills;
    }

    @Transactional(readOnly = true)
    public JobGapResponse analyzeJob(Long jobId) {
        JobOpportunity job = jobs.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Vaga não encontrada: " + jobId));
        return new JobGapResponse(job.getId(), job.getTitle(), job.getCompany(),
                analyzer.analyze(requirementsOf(job), userSkills()));
    }

    /** Todas as vagas ordenadas da mais aderente ao seu perfil para a menos aderente. */
    @Transactional(readOnly = true)
    public List<RankingItem> ranking() {
        List<SkillSpec> userSkills = userSkills();
        Map<Long, ApplicationStatus> statusByJob = statusByJob();
        List<RankingItem> ranking = new ArrayList<>();
        for (JobOpportunity job : jobs.findAllWithRequirements()) {
            GapReport report = analyzer.analyze(requirementsOf(job), userSkills);
            ranking.add(new RankingItem(job.getId(), job.getTitle(), job.getCompany(), job.getWorkMode(),
                    report.score(), report.verdict(), report.missingMandatory(), statusByJob.get(job.getId())));
        }
        ranking.sort(Comparator.comparingInt(RankingItem::score).reversed()
                .thenComparing(RankingItem::title, String.CASE_INSENSITIVE_ORDER));
        return ranking;
    }

    /**
     * O que estudar primeiro: lacunas agregadas das vagas ainda "vivas" (sem candidatura encerrada),
     * ordenadas pela frequência com que aparecem como obrigatórias.
     */
    @Transactional(readOnly = true)
    public List<GapPriority> studyPriorities() {
        List<SkillSpec> userSkills = userSkills();
        Map<Long, ApplicationStatus> statusByJob = statusByJob();
        List<GapReport> reports = new ArrayList<>();
        for (JobOpportunity job : jobs.findAllWithRequirements()) {
            ApplicationStatus status = statusByJob.get(job.getId());
            if (status != null && status.isTerminal()) {
                continue;
            }
            reports.add(analyzer.analyze(requirementsOf(job), userSkills));
        }
        return analyzer.aggregateGaps(reports);
    }

    /** Sugere requisitos a partir do texto da vaga (regras determinísticas; o usuário revisa). */
    public List<Suggestion> extractRequirements(String description) {
        return extractor.extract(description);
    }

    private Map<Long, ApplicationStatus> statusByJob() {
        Map<Long, ApplicationStatus> map = new HashMap<>();
        for (JobApplication application : applications.findAllWithJob()) {
            map.put(application.getJob().getId(), application.getStatus());
        }
        return map;
    }

    private List<RequirementSpec> requirementsOf(JobOpportunity job) {
        return job.getRequirements().stream()
                .map(r -> new RequirementSpec(r.getName(), r.isMandatory(), r.getMinLevel()))
                .toList();
    }

    private List<SkillSpec> userSkills() {
        return skills.findAll().stream().map(s -> new SkillSpec(s.getName(), s.getLevel())).toList();
    }
}
