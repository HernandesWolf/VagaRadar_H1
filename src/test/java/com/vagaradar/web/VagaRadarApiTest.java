package com.vagaradar.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de ponta a ponta da API (HTTP → serviços → JPA → H2 com Flyway).
 * Cada teste roda em uma transação que sofre rollback, então não deixam resíduos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VagaRadarApiTest {

    @Autowired
    private MockMvc mvc;

    // ---------- helpers ----------

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createJob(String title, String workMode, String requirementsJson) throws Exception {
        String json = """
                {"title":"%s","company":"Empresa Teste","location":"Fortaleza, CE","workMode":"%s",
                 "salaryMin":4000,"salaryMax":6000,"requirements":%s}
                """.formatted(title, workMode, requirementsJson);
        return idOf(postJson("/api/jobs", json).andExpect(status().isCreated()));
    }

    // ---------- testes ----------

    @Test
    void analiseDeLacunasComparaPerfilEVaga() throws Exception {
        postJson("/api/skills", "{\"name\":\"java\",\"level\":\"INTERMEDIATE\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java"));

        long jobId = createJob("Dev Java", "REMOTE", """
                [{"name":"Java","mandatory":true,"minLevel":"BEGINNER"},
                 {"name":"Kafka","mandatory":true,"minLevel":"BEGINNER"}]
                """);

        mvc.perform(get("/api/jobs/" + jobId + "/gap-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.score").value(50))
                .andExpect(jsonPath("$.report.missingMandatory[0]").value("Kafka"))
                .andExpect(jsonPath("$.report.metCount").value(1));
    }

    @Test
    void competenciaDuplicadaPorApelidoGeraConflito() throws Exception {
        postJson("/api/skills", "{\"name\":\"PostgreSQL\",\"level\":\"BEGINNER\"}").andExpect(status().isCreated());
        postJson("/api/skills", "{\"name\":\"postgres\",\"level\":\"ADVANCED\"}").andExpect(status().isConflict());
    }

    @Test
    void dadosInvalidosRetornam400ComDetalhePorCampo() throws Exception {
        postJson("/api/jobs", "{\"title\":\"\",\"company\":\"X\",\"workMode\":\"REMOTE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void salarioMaximoMenorQueMinimoERejeitado() throws Exception {
        postJson("/api/jobs", """
                {"title":"Vaga","company":"X","workMode":"REMOTE","salaryMin":5000,"salaryMax":1000}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void filtrosCombinadosDeTecnologiaEModalidade() throws Exception {
        createJob("Vaga Remota Postgres", "REMOTE", "[{\"name\":\"PostgreSQL\",\"mandatory\":true}]");
        createJob("Vaga Presencial Postgres", "ONSITE", "[{\"name\":\"PostgreSQL\",\"mandatory\":true}]");
        createJob("Vaga Remota Python", "REMOTE", "[{\"name\":\"Python\",\"mandatory\":true}]");

        mvc.perform(get("/api/jobs").param("technology", "postgres").param("workMode", "REMOTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Vaga Remota Postgres"));

        mvc.perform(get("/api/jobs").param("q", "python"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(get("/api/jobs").param("location", "fortaleza"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void ordenacaoInvalidaRetorna400() throws Exception {
        mvc.perform(get("/api/jobs").param("sortBy", "senhaDoBanco")).andExpect(status().isBadRequest());
    }

    @Test
    void atualizarVagaSincronizaRequisitosSemViolarUnicidade() throws Exception {
        long jobId = createJob("Dev", "HYBRID", """
                [{"name":"Java","mandatory":true},{"name":"Docker","mandatory":false}]
                """);

        // mantém Java (agora BEGINNER→ADVANCED), remove Docker e adiciona Redis
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/jobs/" + jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dev","company":"Empresa Teste","workMode":"HYBRID",
                                 "requirements":[{"name":"java","mandatory":true,"minLevel":"ADVANCED"},
                                                 {"name":"Redis","mandatory":false}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements.length()").value(2))
                .andExpect(jsonPath("$.requirements[0].name").value("Java"))
                .andExpect(jsonPath("$.requirements[0].minLevel").value("ADVANCED"));
    }

    @Test
    void ciclodeVidaDaCandidaturaRespeitaAMaquinaDeEstados() throws Exception {
        long jobId = createJob("Dev Backend", "REMOTE", "[{\"name\":\"Java\",\"mandatory\":true}]");

        long appId = idOf(postJson("/api/applications", "{\"jobId\":" + jobId + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED")));

        // não pode pular direto para OFFER
        postJson("/api/applications/" + appId + "/status", "{\"status\":\"OFFER\"}")
                .andExpect(status().isConflict());

        postJson("/api/applications/" + appId + "/status", "{\"status\":\"SCREENING\",\"note\":\"RH ligou\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCREENING"))
                .andExpect(jsonPath("$.notes").value(org.hamcrest.Matchers.containsString("RH ligou")));

        // segunda candidatura para a mesma vaga é rejeitada
        postJson("/api/applications", "{\"jobId\":" + jobId + "}").andExpect(status().isConflict());

        // entrevista + contato aparecem no detalhe
        postJson("/api/applications/" + appId + "/interviews", """
                {"interviewType":"TECHNICAL","scheduledAt":"2099-01-10T14:00","interviewer":"Time","location":"Meet"}
                """).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("SCHEDULED"));
        postJson("/api/applications/" + appId + "/contacts", "{\"name\":\"Ana Souza\",\"email\":\"ana@exemplo.com\"}")
                .andExpect(status().isCreated());

        mvc.perform(get("/api/applications/" + appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviews.length()").value(1))
                .andExpect(jsonPath("$.contacts[0].name").value("Ana Souza"));

        // estatísticas: 1 enviada, 1 respondida, 1 com entrevista
        mvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submittedApplications").value(1))
                .andExpect(jsonPath("$.respondedApplications").value(1))
                .andExpect(jsonPath("$.interviewedApplications").value(1))
                .andExpect(jsonPath("$.interviewRate").value(100.0));
    }

    @Test
    void excluirVagaRemoveACandidaturaAssociada() throws Exception {
        long jobId = createJob("Dev", "REMOTE", "[]");
        long appId = idOf(postJson("/api/applications", "{\"jobId\":" + jobId + "}").andExpect(status().isCreated()));

        mvc.perform(delete("/api/jobs/" + jobId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/applications/" + appId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/jobs/" + jobId)).andExpect(status().isNotFound());
    }

    @Test
    void extratorSugereRequisitosAPartirDoTexto() throws Exception {
        postJson("/api/jobs/extract-requirements", """
                {"description":"Requisitos: Java e Spring Boot. Diferenciais: Docker."}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Java')].mandatory").value(true))
                .andExpect(jsonPath("$[?(@.name=='Docker')].mandatory").value(false));
    }

    @Test
    void alertaDePrazoApareceParaVagaSemCandidatura() throws Exception {
        String tomorrow = java.time.LocalDate.now().plusDays(1).toString();
        postJson("/api/jobs", """
                {"title":"Vaga Urgente","company":"X","workMode":"REMOTE","deadline":"%s"}
                """.formatted(tomorrow)).andExpect(status().isCreated());

        mvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='JOB_DEADLINE')].severity").value("CRITICAL"));
    }
}
