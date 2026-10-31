package com.vagaradar.analysis;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Catálogo de tecnologias conhecidas, com apelidos (ex.: "postgres" → PostgreSQL, "k8s" → Kubernetes).
 *
 * <p>Serve a dois propósitos:
 * <ol>
 *   <li><b>Normalização</b>: "Spring-Boot", "springboot" e "Spring Boot" viram a mesma competência,
 *       para que a comparação entre vaga e perfil não falhe por causa de grafia.</li>
 *   <li><b>Detecção</b>: encontra tecnologias em textos livres (descrição de vaga).</li>
 * </ol>
 *
 * <p>Classe pura (sem Spring): fácil de testar e de evoluir.
 */
public final class SkillCatalog {

    /** Tecnologia encontrada em um texto. */
    public record Match(String canonicalName, int start, int end) {
    }

    private record Alias(String canonical, Pattern pattern, int length) {
    }

    private static final Map<String, String> CANONICAL_BY_ALIAS = new HashMap<>();
    private static final List<Alias> ALIASES = new ArrayList<>();

    static {
        // Linguagens
        add("Java", "jdk", "jvm");
        add("Kotlin");
        add("Python");
        add("JavaScript", "js", "ecmascript");
        add("TypeScript");
        add("C#", "csharp");
        add(".NET", "dotnet", "asp.net", ".net core");
        add("PHP");
        add("Golang");
        add("Rust");
        add("HTML", "html5");
        add("CSS", "css3");

        // Ecossistema Java / Spring
        add("Spring Boot", "springboot");
        add("Spring Data JPA", "spring data");
        add("Spring Security");
        add("Spring Cloud");
        add("Spring", "spring framework");
        add("Hibernate");
        add("JPA", "jakarta persistence");
        add("Maven");
        add("Gradle");
        add("JUnit", "junit5", "junit 5");
        add("Mockito");
        add("Testcontainers");
        add("Flyway");
        add("Liquibase");

        // Web / APIs
        add("REST", "restful", "api rest", "apis rest", "rest api", "rest apis");
        add("GraphQL");
        add("Swagger", "openapi");
        add("JWT");
        add("OAuth", "oauth2");
        add("Microsserviços", "microservices", "microservicos", "microsservicos");
        add("React", "reactjs", "react.js");
        add("Angular", "angularjs");
        add("Vue", "vue.js", "vuejs");
        add("Node.js", "nodejs", "node");
        add("Laravel");

        // Bancos de dados e mensageria
        add("SQL");
        add("PostgreSQL", "postgres");
        add("MySQL");
        add("Oracle", "pl/sql", "plsql");
        add("SQL Server", "sqlserver", "mssql");
        add("MongoDB", "mongo");
        add("NoSQL");
        add("Redis");
        add("Kafka", "apache kafka");
        add("RabbitMQ");

        // DevOps / Cloud
        add("Docker");
        add("Kubernetes", "k8s");
        add("AWS", "amazon web services");
        add("Azure");
        add("GCP", "google cloud");
        add("Git");
        add("GitHub Actions");
        add("CI/CD", "ci cd", "cicd", "integracao continua");
        add("Jenkins");
        add("Terraform");
        add("Linux");

        // Práticas e fundamentos
        add("Scrum");
        add("Agile", "metodologias ageis", "agil");
        add("TDD");
        add("Clean Code", "codigo limpo");
        add("SOLID");
        add("Design Patterns", "padroes de projeto");
        add("DDD", "domain-driven design", "domain driven design");
        add("POO", "oop", "orientacao a objetos");
        add("Estruturas de Dados", "estrutura de dados", "algoritmos");
        add("Inglês", "english");

        // Mais específicos primeiro não é necessário: a resolução de sobreposição fica a cargo de scan().
    }

    private SkillCatalog() {
    }

    private static void add(String canonical, String... aliases) {
        register(canonical, canonical);
        for (String alias : aliases) {
            register(canonical, alias);
        }
    }

    private static void register(String canonical, String alias) {
        String normalized = normalize(alias);
        CANONICAL_BY_ALIAS.put(normalized, canonical);
        Pattern pattern = Pattern.compile(
                "(?<![\\p{L}\\p{N}])" + Pattern.quote(normalized) + "(?![\\p{L}\\p{N}])");
        ALIASES.add(new Alias(canonical, pattern, normalized.length()));
    }

    /** Minúsculas, sem acentos, espaços colapsados. Base de toda comparação de nomes. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(raw, Normalizer.Form.NFD);
        String withoutAccents = decomposed.replaceAll("\\p{M}+", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** Nome de exibição: o do catálogo, quando conhecido; senão o texto original limpo. */
    public static String canonicalName(String raw) {
        if (raw == null) {
            return "";
        }
        String known = CANONICAL_BY_ALIAS.get(normalize(raw));
        if (known != null) {
            return known;
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    /** Chave de comparação estável (idempotente): key(key(x)) == key(x). */
    public static String key(String raw) {
        return normalize(canonicalName(raw));
    }

    /**
     * Encontra tecnologias em um texto livre. Quando duas ocorrências se sobrepõem
     * (ex.: "Spring" dentro de "Spring Boot"), vence a mais longa.
     */
    public static List<Match> scan(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = normalize(text);
        List<Match> candidates = new ArrayList<>();
        for (Alias alias : ALIASES) {
            Matcher matcher = alias.pattern().matcher(normalized);
            while (matcher.find()) {
                candidates.add(new Match(alias.canonical(), matcher.start(), matcher.end()));
            }
        }
        candidates.sort(Comparator
                .comparingInt((Match m) -> m.end() - m.start()).reversed()
                .thenComparingInt(Match::start));

        List<Match> accepted = new ArrayList<>();
        for (Match candidate : candidates) {
            boolean overlaps = false;
            for (Match kept : accepted) {
                if (candidate.start() < kept.end() && kept.start() < candidate.end()) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                accepted.add(candidate);
            }
        }
        accepted.sort(Comparator.comparingInt(Match::start));
        return accepted;
    }
}
