package com.vagaradar.analysis;

import com.vagaradar.domain.SkillLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor determinístico (sem IA) que compara os requisitos de uma vaga com as competências
 * declaradas pelo usuário e aponta as lacunas de qualificação.
 *
 * <h3>Regras</h3>
 * <ul>
 *   <li>Requisito <b>obrigatório</b> pesa 3; <b>desejável</b> pesa 1.</li>
 *   <li>Nível do usuário &ge; nível exigido → crédito 100% (<i>MET</i>).</li>
 *   <li>Nível menor que o exigido → crédito proporcional: {@code nívelUsuário / nívelExigido} (<i>PARTIAL</i>).</li>
 *   <li>Competência ausente → crédito 0 (<i>MISSING</i>).</li>
 *   <li>Score = soma(peso × crédito) / soma(pesos) × 100, arredondado.</li>
 * </ul>
 *
 * <p>Classe pura, sem dependências de framework: 100% testável com JUnit simples.
 */
public final class GapAnalyzer {

    public static final int WEIGHT_MANDATORY = 3;
    public static final int WEIGHT_OPTIONAL = 1;

    public enum MatchStatus {
        MET, PARTIAL, MISSING
    }

    /** Requisito de uma vaga. {@code minLevel} nulo equivale a BEGINNER. */
    public record RequirementSpec(String name, boolean mandatory, SkillLevel minLevel) {
        public SkillLevel effectiveMinLevel() {
            return minLevel == null ? SkillLevel.BEGINNER : minLevel;
        }
    }

    /** Competência declarada pelo usuário. */
    public record SkillSpec(String name, SkillLevel level) {
    }

    public record RequirementResult(String name,
                                    boolean mandatory,
                                    SkillLevel requiredLevel,
                                    SkillLevel userLevel,
                                    MatchStatus status,
                                    double credit) {
    }

    public record GapReport(int score,
                            String verdict,
                            int metCount,
                            int partialCount,
                            int missingCount,
                            List<RequirementResult> results,
                            List<String> missingMandatory,
                            List<String> missingOptional,
                            List<String> toImprove) {
    }

    /** Lacuna agregada entre várias vagas: ajuda a decidir o que estudar primeiro. */
    public record GapPriority(String skill, int jobsAffected, int mandatoryJobs, int priority) {
    }

    public GapReport analyze(List<RequirementSpec> requirements, List<SkillSpec> skills) {
        Map<String, SkillLevel> byKey = new LinkedHashMap<>();
        for (SkillSpec skill : skills) {
            SkillLevel level = skill.level() == null ? SkillLevel.BEGINNER : skill.level();
            // Se houver duplicatas, vale o maior nível.
            byKey.merge(SkillCatalog.key(skill.name()), level,
                    (a, b) -> a.rank() >= b.rank() ? a : b);
        }

        List<RequirementResult> results = new ArrayList<>();
        List<String> missingMandatory = new ArrayList<>();
        List<String> missingOptional = new ArrayList<>();
        List<String> toImprove = new ArrayList<>();
        int met = 0;
        int partial = 0;
        int missing = 0;
        double earned = 0;
        double total = 0;

        for (RequirementSpec requirement : requirements) {
            SkillLevel required = requirement.effectiveMinLevel();
            SkillLevel userLevel = byKey.get(SkillCatalog.key(requirement.name()));
            int weight = requirement.mandatory() ? WEIGHT_MANDATORY : WEIGHT_OPTIONAL;

            MatchStatus status;
            double credit;
            if (userLevel == null) {
                status = MatchStatus.MISSING;
                credit = 0;
                missing++;
                (requirement.mandatory() ? missingMandatory : missingOptional).add(requirement.name());
            } else if (userLevel.rank() >= required.rank()) {
                status = MatchStatus.MET;
                credit = 1;
                met++;
            } else {
                status = MatchStatus.PARTIAL;
                credit = (double) userLevel.rank() / required.rank();
                partial++;
                toImprove.add(requirement.name());
            }

            earned += weight * credit;
            total += weight;
            results.add(new RequirementResult(requirement.name(), requirement.mandatory(),
                    required, userLevel, status, round2(credit)));
        }

        int score = total == 0 ? 0 : (int) Math.round(earned / total * 100.0);
        return new GapReport(score, verdict(score, requirements.isEmpty()), met, partial, missing,
                results, missingMandatory, missingOptional, toImprove);
    }

    /**
     * Agrega as lacunas (ausentes e parciais) de várias análises. A prioridade de estudo é maior
     * para competências que aparecem como obrigatórias em muitas vagas.
     */
    public List<GapPriority> aggregateGaps(List<GapReport> reports) {
        Map<String, int[]> counters = new LinkedHashMap<>(); // [vagas afetadas, vagas onde é obrigatória]
        Map<String, String> displayNames = new LinkedHashMap<>();
        for (GapReport report : reports) {
            for (RequirementResult result : report.results()) {
                if (result.status() == MatchStatus.MET) {
                    continue;
                }
                String key = SkillCatalog.key(result.name());
                displayNames.putIfAbsent(key, result.name());
                int[] counter = counters.computeIfAbsent(key, k -> new int[2]);
                counter[0]++;
                if (result.mandatory()) {
                    counter[1]++;
                }
            }
        }
        List<GapPriority> priorities = new ArrayList<>();
        counters.forEach((key, c) -> priorities.add(new GapPriority(
                displayNames.get(key), c[0], c[1],
                c[1] * WEIGHT_MANDATORY + (c[0] - c[1]) * WEIGHT_OPTIONAL)));
        priorities.sort(Comparator
                .comparingInt(GapPriority::priority).reversed()
                .thenComparing(GapPriority::skill, String.CASE_INSENSITIVE_ORDER));
        return priorities;
    }

    private static String verdict(int score, boolean noRequirements) {
        if (noRequirements) {
            return "Sem requisitos cadastrados";
        }
        if (score >= 85) {
            return "Excelente aderência";
        }
        if (score >= 65) {
            return "Boa aderência";
        }
        if (score >= 40) {
            return "Aderência parcial";
        }
        return "Baixa aderência";
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
