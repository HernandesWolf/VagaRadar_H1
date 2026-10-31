package com.vagaradar.config;

import com.vagaradar.analysis.GapAnalyzer;
import com.vagaradar.analysis.RequirementExtractor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** Relógio injetável: permite testar regras que dependem de "hoje" sem depender da data real. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public GapAnalyzer gapAnalyzer() {
        return new GapAnalyzer();
    }

    @Bean
    public RequirementExtractor requirementExtractor() {
        return new RequirementExtractor();
    }

    @Bean
    public OpenAPI vagaRadarOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VagaRadar API")
                .version("1.0.0")
                .description("Acompanhamento de vagas, candidaturas e lacunas de qualificação.")
                .license(new License().name("MIT")));
    }
}
