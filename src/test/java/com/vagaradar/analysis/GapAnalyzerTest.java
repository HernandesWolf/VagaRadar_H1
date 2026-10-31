package com.vagaradar.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vagaradar.analysis.GapAnalyzer.GapPriority;
import com.vagaradar.analysis.GapAnalyzer.GapReport;
import com.vagaradar.analysis.GapAnalyzer.MatchStatus;
import com.vagaradar.analysis.GapAnalyzer.RequirementSpec;
import com.vagaradar.analysis.GapAnalyzer.SkillSpec;
import com.vagaradar.domain.SkillLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

class GapAnalyzerTest {

    private final GapAnalyzer analyzer = new GapAnalyzer();

    @Test
    void perfilQueAtendeTudoTemScore100() {
        GapReport report = analyzer.analyze(
                List.of(new RequirementSpec("Java", true, SkillLevel.INTERMEDIATE),
                        new RequirementSpec("Docker", false, SkillLevel.BEGINNER)),
                List.of(new SkillSpec("java", SkillLevel.ADVANCED),
                        new SkillSpec("Docker", SkillLevel.BEGINNER)));

        assertEquals(100, report.score());
        assertEquals(2, report.metCount());
        assertEquals("Excelente aderência", report.verdict());
        assertTrue(report.missingMandatory().isEmpty());
    }

    @Test
    void competenciaAusenteZeraOCreditoEApareceNasLacunas() {
        GapReport report = analyzer.analyze(
                List.of(new RequirementSpec("Java", true, SkillLevel.BEGINNER),
                        new RequirementSpec("Kafka", true, SkillLevel.BEGINNER),
                        new RequirementSpec("Redis", false, SkillLevel.BEGINNER)),
                List.of(new SkillSpec("Java", SkillLevel.BEGINNER)));

        // pesos: Java 3 (ok) + Kafka 3 (falta) + Redis 1 (falta) => 3/7 = 43%
        assertEquals(43, report.score());
        assertEquals(List.of("Kafka"), report.missingMandatory());
        assertEquals(List.of("Redis"), report.missingOptional());
        assertEquals("Aderência parcial", report.verdict());
    }

    @Test
    void nivelInferiorGeraCreditoProporcional() {
        GapReport report = analyzer.analyze(
                List.of(new RequirementSpec("Spring Boot", true, SkillLevel.ADVANCED)),
                List.of(new SkillSpec("springboot", SkillLevel.BEGINNER)));

        assertEquals(33, report.score()); // 1/3
        assertEquals(MatchStatus.PARTIAL, report.results().get(0).status());
        assertEquals(List.of("Spring Boot"), report.toImprove());
    }

    @Test
    void obrigatorioPesaMaisQueDesejavel() {
        List<SkillSpec> skills = List.of(new SkillSpec("Java", SkillLevel.INTERMEDIATE));
        GapReport faltaObrigatorio = analyzer.analyze(
                List.of(new RequirementSpec("Java", true, SkillLevel.BEGINNER),
                        new RequirementSpec("Kafka", true, SkillLevel.BEGINNER)), skills);
        GapReport faltaDesejavel = analyzer.analyze(
                List.of(new RequirementSpec("Java", true, SkillLevel.BEGINNER),
                        new RequirementSpec("Kafka", false, SkillLevel.BEGINNER)), skills);

        assertTrue(faltaDesejavel.score() > faltaObrigatorio.score());
    }

    @Test
    void semRequisitosNaoQuebra() {
        GapReport report = analyzer.analyze(List.of(), List.of());
        assertEquals(0, report.score());
        assertEquals("Sem requisitos cadastrados", report.verdict());
    }

    @Test
    void apelidosSaoReconhecidosNaComparacao() {
        GapReport report = analyzer.analyze(
                List.of(new RequirementSpec("PostgreSQL", true, SkillLevel.BEGINNER)),
                List.of(new SkillSpec("postgres", SkillLevel.INTERMEDIATE)));
        assertEquals(100, report.score());
    }

    @Test
    void agregaLacunasEPriorizaObrigatoriasMaisFrequentes() {
        List<SkillSpec> skills = List.of(new SkillSpec("Java", SkillLevel.INTERMEDIATE));
        GapReport vaga1 = analyzer.analyze(
                List.of(new RequirementSpec("Java", true, SkillLevel.BEGINNER),
                        new RequirementSpec("Kafka", true, SkillLevel.BEGINNER),
                        new RequirementSpec("AWS", false, SkillLevel.BEGINNER)), skills);
        GapReport vaga2 = analyzer.analyze(
                List.of(new RequirementSpec("Kafka", true, SkillLevel.BEGINNER),
                        new RequirementSpec("AWS", true, SkillLevel.BEGINNER)), skills);

        List<GapPriority> priorities = analyzer.aggregateGaps(List.of(vaga1, vaga2));

        assertEquals("Kafka", priorities.get(0).skill());
        assertEquals(2, priorities.get(0).jobsAffected());
        assertEquals(6, priorities.get(0).priority());
        assertEquals("AWS", priorities.get(1).skill());
        assertEquals(4, priorities.get(1).priority()); // 1 obrigatória (3) + 1 desejável (1)
    }
}
