package com.vagaradar.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Garante que a carga de demonstração roda de ponta a ponta e produz os números esperados. */
@SpringBootTest(properties = {
        "vagaradar.demo-data=true",
        "spring.datasource.url=jdbc:h2:mem:vagaradar-demo-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoDataTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void estatisticasDaDemonstracao() throws Exception {
        mvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalJobs").value(8))
                .andExpect(jsonPath("$.totalApplications").value(6))
                .andExpect(jsonPath("$.submittedApplications").value(5))
                .andExpect(jsonPath("$.respondedApplications").value(4))
                .andExpect(jsonPath("$.interviewedApplications").value(2))
                .andExpect(jsonPath("$.offers").value(1))
                .andExpect(jsonPath("$.responseRate").value(80.0))
                .andExpect(jsonPath("$.interviewRate").value(40.0));
    }

    @Test
    void alertasDaDemonstracao() throws Exception {
        mvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='STALE_APPLICATION')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.type=='FOLLOW_UP_DUE')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.type=='INTERVIEW_UPCOMING')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.type=='JOB_DEADLINE')]").isNotEmpty());
    }

    @Test
    void rankingEPrioridadesDeEstudo() throws Exception {
        mvc.perform(get("/api/gap-analysis/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(100))
                .andExpect(jsonPath("$.length()").value(8));

        mvc.perform(get("/api/gap-analysis/study-priorities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skill").exists());
    }
}
