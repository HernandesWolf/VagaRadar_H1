package com.vagaradar.analysis;

import com.vagaradar.domain.SkillLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Extrai <i>sugestões</i> de requisitos a partir do texto de uma vaga usando apenas regras:
 * dicionário de tecnologias ({@link SkillCatalog}) + palavras-chave de obrigatoriedade e de nível.
 *
 * <p>É a base determinística para uma etapa futura com LLM: a interface de uso é a mesma
 * (texto entra, lista de sugestões revisáveis sai), então trocar/complementar a implementação
 * não afeta o restante do sistema. O resultado é sempre revisado pelo usuário antes de ser salvo.
 *
 * <h3>Heurísticas</h3>
 * <ul>
 *   <li>Títulos de seção como "Diferenciais" / "Desejável" tornam os itens seguintes <i>opcionais</i>;
 *       "Requisitos" / "Obrigatório" voltam ao modo <i>obrigatório</i>.</li>
 *   <li>Marcadores na própria frase ("é um plus", "nice to have") também a tornam opcional.</li>
 *   <li>Termos como "avançado/sênior" → ADVANCED; "noções/básico" → BEGINNER;
 *       "experiência/pleno" → INTERMEDIATE. Sem marcador: obrigatório = INTERMEDIATE, opcional = BEGINNER.</li>
 * </ul>
 */
public final class RequirementExtractor {

    /** Requisito sugerido, com a frase que o originou ({@code evidence}) para facilitar a revisão. */
    public record Suggestion(String name, boolean mandatory, SkillLevel minLevel, String evidence) {
    }

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("\\R+|(?<=[.;!?])\\s+");
    private static final Pattern OPTIONAL_HEADING = Pattern.compile(
            "^\\W*(diferenciais?|desejavel|desejaveis|nice to have|bonus|preferred|differentials?)\\b.*");
    private static final Pattern REQUIRED_HEADING = Pattern.compile(
            "^\\W*(requisitos?|obrigatorios?|obrigatorio|pre-requisitos?|must have|requirements?|required|"
                    + "qualificacoes|responsabilidades|atividades|o que buscamos|responsibilities)\\b.*");
    private static final Pattern OPTIONAL_MARKER = Pattern.compile(
            "diferencial|desejavel|nice to have|e um plus|a plus|bonus|preferencialmente|sera um plus|vantagem");
    private static final Pattern ADVANCED_MARKER = Pattern.compile(
            "avancad|senior|solid[oa]|forte|expert|especialista|advanced|strong");
    private static final Pattern BEGINNER_MARKER = Pattern.compile(
            "nocoes|nocao|basico|basica|iniciante|familiaridade|junior|basic");
    private static final Pattern INTERMEDIATE_MARKER = Pattern.compile(
            "intermediari|pleno|experiencia|experience|vivencia|intermediate");

    public List<Suggestion> extract(String description) {
        if (description == null || description.isBlank()) {
            return List.of();
        }

        Map<String, Suggestion> byKey = new LinkedHashMap<>();
        boolean optionalSection = false;

        for (String rawSentence : SENTENCE_SPLIT.split(description)) {
            String sentence = rawSentence.trim();
            if (sentence.isEmpty()) {
                continue;
            }
            String normalized = SkillCatalog.normalize(sentence);

            if (OPTIONAL_HEADING.matcher(normalized).matches()) {
                optionalSection = true;
            } else if (REQUIRED_HEADING.matcher(normalized).matches()) {
                optionalSection = false;
            }

            boolean optional = optionalSection || OPTIONAL_MARKER.matcher(normalized).find();
            SkillLevel level = detectLevel(normalized, optional);

            for (SkillCatalog.Match match : SkillCatalog.scan(sentence)) {
                String key = SkillCatalog.key(match.canonicalName());
                Suggestion current = new Suggestion(match.canonicalName(), !optional, level, shorten(sentence));
                byKey.merge(key, current, RequirementExtractor::merge);
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private static SkillLevel detectLevel(String normalized, boolean optional) {
        if (ADVANCED_MARKER.matcher(normalized).find()) {
            return SkillLevel.ADVANCED;
        }
        if (BEGINNER_MARKER.matcher(normalized).find()) {
            return SkillLevel.BEGINNER;
        }
        if (INTERMEDIATE_MARKER.matcher(normalized).find()) {
            return SkillLevel.INTERMEDIATE;
        }
        return optional ? SkillLevel.BEGINNER : SkillLevel.INTERMEDIATE;
    }

    /** Se a mesma tecnologia aparece mais de uma vez, prevalece a exigência mais forte. */
    private static Suggestion merge(Suggestion a, Suggestion b) {
        boolean mandatory = a.mandatory() || b.mandatory();
        SkillLevel level = a.minLevel().rank() >= b.minLevel().rank() ? a.minLevel() : b.minLevel();
        return new Suggestion(a.name(), mandatory, level, a.evidence());
    }

    private static String shorten(String sentence) {
        return sentence.length() <= 140 ? sentence : sentence.substring(0, 137) + "...";
    }
}
