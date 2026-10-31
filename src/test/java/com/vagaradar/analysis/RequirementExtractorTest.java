package com.vagaradar.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vagaradar.analysis.RequirementExtractor.Suggestion;
import com.vagaradar.domain.SkillLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequirementExtractorTest {

    private final RequirementExtractor extractor = new RequirementExtractor();

    private static Suggestion find(List<Suggestion> list, String name) {
        return list.stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
    }

    @Test
    void separaObrigatoriosEDiferenciaisPorSecao() {
        String text = """
                Requisitos:
                - Java 17 e Spring Boot
                - Experiência sólida com PostgreSQL
                Diferenciais:
                - Docker
                - Conhecimento em Kafka
                """;
        List<Suggestion> result = extractor.extract(text);

        assertTrue(find(result, "Java").mandatory());
        assertTrue(find(result, "Spring Boot").mandatory());
        assertTrue(find(result, "PostgreSQL").mandatory());
        assertEquals(SkillLevel.ADVANCED, find(result, "PostgreSQL").minLevel());
        assertFalse(find(result, "Docker").mandatory());
        assertFalse(find(result, "Kafka").mandatory());
    }

    @Test
    void marcadorNaFraseTornaOpcional() {
        List<Suggestion> result = extractor.extract("Java é obrigatório. Conhecer AWS é um plus.");
        assertTrue(find(result, "Java").mandatory());
        assertFalse(find(result, "AWS").mandatory());
    }

    @Test
    void detectaNivelBasico() {
        List<Suggestion> result = extractor.extract("Requisitos: noções de Git");
        assertEquals(SkillLevel.BEGINNER, find(result, "Git").minLevel());
    }

    @Test
    void mesmaTecnologiaRepetidaPrevaleceAExigenciaMaisForte() {
        List<Suggestion> result = extractor.extract("Diferenciais: Docker. Requisitos: Docker avançado.");
        Suggestion docker = find(result, "Docker");
        assertNotNull(docker);
        assertTrue(docker.mandatory());
        assertEquals(SkillLevel.ADVANCED, docker.minLevel());
        assertEquals(1, result.stream().filter(s -> s.name().equals("Docker")).count());
    }

    @Test
    void textoVazioRetornaListaVazia() {
        assertTrue(extractor.extract("   ").isEmpty());
        assertTrue(extractor.extract(null).isEmpty());
    }
}
