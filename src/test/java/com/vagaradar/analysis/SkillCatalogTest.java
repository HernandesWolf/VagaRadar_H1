package com.vagaradar.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SkillCatalogTest {

    @Test
    void normalizaApelidosParaNomeCanonico() {
        assertEquals("PostgreSQL", SkillCatalog.canonicalName("postgres"));
        assertEquals("Kubernetes", SkillCatalog.canonicalName("  K8S "));
        assertEquals("Spring Boot", SkillCatalog.canonicalName("springboot"));
    }

    @Test
    void mantemNomeDesconhecidoLimpo() {
        assertEquals("Tecnologia Nova", SkillCatalog.canonicalName("  Tecnologia   Nova "));
    }

    @Test
    void chaveEIdempotenteEIgnoraAcentoECaixa() {
        assertEquals(SkillCatalog.key("Inglês"), SkillCatalog.key("INGLES"));
        assertEquals(SkillCatalog.key("Spring Boot"), SkillCatalog.key(SkillCatalog.key("Spring Boot")));
    }

    @Test
    void scanNaoConfundeJavaComJavaScript() {
        List<SkillCatalog.Match> matches = SkillCatalog.scan("Conhecimento em JavaScript e HTML");
        List<String> names = matches.stream().map(SkillCatalog.Match::canonicalName).toList();
        assertTrue(names.contains("JavaScript"));
        assertTrue(names.contains("HTML"));
        assertTrue(!names.contains("Java"), "Java não deveria casar dentro de JavaScript");
    }

    @Test
    void scanPrefereOMatchMaisLongo() {
        List<String> names = SkillCatalog.scan("Experiência com Spring Boot").stream()
                .map(SkillCatalog.Match::canonicalName).toList();
        assertEquals(List.of("Spring Boot"), names);
    }

    @Test
    void scanReconheceSimbolosEspeciais() {
        List<String> names = SkillCatalog.scan("Vivência com C#, .NET e Node.js.").stream()
                .map(SkillCatalog.Match::canonicalName).toList();
        assertTrue(names.contains("C#"));
        assertTrue(names.contains(".NET"));
        assertTrue(names.contains("Node.js"));
    }
}
