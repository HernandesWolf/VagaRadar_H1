package com.vagaradar.config;

import com.vagaradar.domain.ApplicationStatus;
import com.vagaradar.domain.InterviewStatus;
import com.vagaradar.domain.InterviewType;
import com.vagaradar.domain.SkillLevel;
import com.vagaradar.domain.StageStatus;
import com.vagaradar.domain.StageType;
import com.vagaradar.domain.WorkMode;
import com.vagaradar.repository.JobApplicationRepository;
import com.vagaradar.repository.JobOpportunityRepository;
import com.vagaradar.repository.UserSkillRepository;
import com.vagaradar.service.ApplicationService;
import com.vagaradar.service.JobService;
import com.vagaradar.service.ProcessService;
import com.vagaradar.service.SkillService;
import com.vagaradar.web.dto.ApplicationDtos.ChangeStatusRequest;
import com.vagaradar.web.dto.ApplicationDtos.ContactRequest;
import com.vagaradar.web.dto.ApplicationDtos.CreateApplicationRequest;
import com.vagaradar.web.dto.ApplicationDtos.InterviewRequest;
import com.vagaradar.web.dto.ApplicationDtos.StageRequest;
import com.vagaradar.web.dto.JobDtos.JobRequest;
import com.vagaradar.web.dto.JobDtos.RequirementRequest;
import com.vagaradar.web.dto.SkillDtos.SkillRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Popula o banco com dados <b>fictícios</b> para demonstração (empresas e pessoas inventadas).
 * Só roda com {@code vagaradar.demo-data=true} e apenas se o banco estiver vazio.
 * As datas são relativas a "hoje", para que os alertas apareçam sempre.
 */
@Component
@ConditionalOnProperty(name = "vagaradar.demo-data", havingValue = "true")
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final SkillService skills;
    private final JobService jobs;
    private final ApplicationService applications;
    private final ProcessService process;
    private final UserSkillRepository skillRepository;
    private final JobOpportunityRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final Clock clock;
    private Long lastJobId;

    public DemoDataLoader(SkillService skills, JobService jobs, ApplicationService applications,
                          ProcessService process, UserSkillRepository skillRepository,
                          JobOpportunityRepository jobRepository,
                          JobApplicationRepository applicationRepository, Clock clock) {
        this.skills = skills;
        this.jobs = jobs;
        this.applications = applications;
        this.process = process;
        this.skillRepository = skillRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (jobRepository.count() > 0 || skillRepository.count() > 0) {
            log.info("Banco já possui dados: carga de demonstração ignorada.");
            return;
        }
        LocalDate today = LocalDate.now(clock);

        // --- Competências do usuário (perfil júnior em Java) ---
        skill("Java", SkillLevel.INTERMEDIATE);
        skill("Spring Boot", SkillLevel.BEGINNER);
        skill("SQL", SkillLevel.INTERMEDIATE);
        skill("PostgreSQL", SkillLevel.BEGINNER);
        skill("Git", SkillLevel.INTERMEDIATE);
        skill("HTML", SkillLevel.INTERMEDIATE);
        skill("CSS", SkillLevel.INTERMEDIATE);
        skill("JavaScript", SkillLevel.BEGINNER);
        skill("REST", SkillLevel.BEGINNER);
        skill("Docker", SkillLevel.BEGINNER);
        skill("POO", SkillLevel.INTERMEDIATE);
        skill("Inglês", SkillLevel.INTERMEDIATE);

        // --- 1) Lista de desejos, prazo se aproximando ---
        job("Desenvolvedor Java Júnior", "Aurora Sistemas", "Fortaleza, CE", WorkMode.HYBRID, 4000, 5500,
                today.plusDays(3),
                "Requisitos: Java 17 e Spring Boot. Conhecimento básico de SQL e Git.\n"
                        + "Diferenciais: Docker e PostgreSQL.",
                req("Java", true, SkillLevel.INTERMEDIATE), req("Spring Boot", true, SkillLevel.BEGINNER),
                req("SQL", true, SkillLevel.BEGINNER), req("Git", true, SkillLevel.BEGINNER),
                req("PostgreSQL", false, SkillLevel.BEGINNER), req("Docker", false, SkillLevel.BEGINNER));
        Long j1 = lastJobId();
        applications.create(new CreateApplicationRequest(j1, ApplicationStatus.WISHLIST, null, null,
                "Vaga com boa aderência ao meu perfil. Preparar carta de apresentação."));

        // --- 2) Enviada há 18 dias, sem resposta e sem follow-up (alerta de candidatura parada) ---
        job("Analista de Desenvolvimento Backend", "Vetor Bank", "Remoto", WorkMode.REMOTE, 6000, 8000,
                today.plusDays(14),
                "Requisitos: Java avançado, Spring Boot, Kafka e PostgreSQL.\nDiferenciais: AWS e Kubernetes.",
                req("Java", true, SkillLevel.ADVANCED), req("Spring Boot", true, SkillLevel.INTERMEDIATE),
                req("Kafka", true, SkillLevel.BEGINNER), req("PostgreSQL", true, SkillLevel.INTERMEDIATE),
                req("Docker", true, SkillLevel.BEGINNER), req("AWS", false, SkillLevel.BEGINNER),
                req("Kubernetes", false, SkillLevel.BEGINNER));
        Long a2 = application(lastJobId(), ApplicationStatus.APPLIED, today.minusDays(18), null,
                "Candidatura pelo site de carreiras.");
        backdate(a2, today.minusDays(18));

        // --- 3) Em entrevistas, com entrevista marcada para daqui a 2 dias ---
        job("Desenvolvedor Full Stack Pleno", "Nimbus Cloud", "São Paulo, SP", WorkMode.HYBRID, 7000, 9500,
                today.plusDays(20),
                "Requisitos: Java, Spring Boot, React e TypeScript. Docker é desejável.",
                req("Java", true, SkillLevel.INTERMEDIATE), req("Spring Boot", true, SkillLevel.INTERMEDIATE),
                req("React", true, SkillLevel.INTERMEDIATE), req("TypeScript", true, SkillLevel.BEGINNER),
                req("Docker", true, SkillLevel.BEGINNER), req("AWS", false, SkillLevel.BEGINNER));
        Long a3 = application(lastJobId(), ApplicationStatus.APPLIED, today.minusDays(12), null, null);
        advance(a3, ApplicationStatus.SCREENING, ApplicationStatus.INTERVIEWING);
        stage(a3, StageType.HR_SCREENING, "Conversa com RH", StageStatus.PASSED, today.minusDays(9));
        stage(a3, StageType.TECHNICAL_TEST, "Desafio técnico (API REST)", StageStatus.PASSED, today.minusDays(3));
        stage(a3, StageType.TECHNICAL_INTERVIEW, "Entrevista técnica", StageStatus.IN_PROGRESS, today.plusDays(2));
        stage(a3, StageType.FINAL_INTERVIEW, "Entrevista com o gestor", StageStatus.PENDING, today.plusDays(9));
        interview(a3, InterviewType.HR, today.minusDays(9).atTime(10, 0), "Camila (RH)", "Google Meet",
                InterviewStatus.COMPLETED, "Conversa tranquila; perguntaram sobre pretensão e disponibilidade.");
        interview(a3, InterviewType.TECHNICAL, today.plusDays(2).atTime(14, 0), "Time de Engenharia",
                "Google Meet", InterviewStatus.SCHEDULED, null);
        contact(a3, "Camila Rocha", "Recrutadora", "camila.rocha@nimbus.example", "https://www.linkedin.com/in/exemplo");

        // --- 4) Em triagem, follow-up vencido ontem ---
        job("Desenvolvedor Backend Java", "Pixel Saúde", "Recife, PE", WorkMode.ONSITE, 5000, 6500,
                today.plusDays(5),
                "Requisitos: Java, Spring Boot, MySQL e APIs REST. Diferencial: JUnit.",
                req("Java", true, SkillLevel.INTERMEDIATE), req("Spring Boot", true, SkillLevel.BEGINNER),
                req("MySQL", true, SkillLevel.BEGINNER), req("REST", true, SkillLevel.BEGINNER),
                req("JUnit", false, SkillLevel.BEGINNER));
        Long a4 = application(lastJobId(), ApplicationStatus.APPLIED, today.minusDays(8), today.minusDays(1), null);
        advance(a4, ApplicationStatus.SCREENING);
        stage(a4, StageType.HR_SCREENING, "Triagem por telefone", StageStatus.IN_PROGRESS, today.plusDays(1));
        contact(a4, "Rafael Lima", "Gestor de Tecnologia", "rafael.lima@pixelsaude.example", null);

        // --- 5) Recusada ---
        job("Estagiário de Desenvolvimento", "Faísca Labs", "Remoto", WorkMode.REMOTE, 1800, 2200,
                today.minusDays(20),
                "Requisitos: noções de Java, Git e SQL. Inglês é um plus.",
                req("Java", true, SkillLevel.BEGINNER), req("Git", true, SkillLevel.BEGINNER),
                req("SQL", true, SkillLevel.BEGINNER), req("Inglês", false, SkillLevel.INTERMEDIATE));
        Long a5 = application(lastJobId(), ApplicationStatus.APPLIED, today.minusDays(30), null, null);
        advance(a5, ApplicationStatus.REJECTED);
        backdate(a5, today.minusDays(22));

        // --- 6) Sem candidatura ainda ---
        job("Engenheiro de Software Júnior", "Órbita Logística", "Fortaleza, CE", WorkMode.ONSITE, 4500, 6000,
                today.plusDays(10),
                "Requisitos: Java, Spring Boot e PostgreSQL.\nDiferenciais: CI/CD e microsserviços.",
                req("Java", true, SkillLevel.INTERMEDIATE), req("Spring Boot", true, SkillLevel.BEGINNER),
                req("PostgreSQL", true, SkillLevel.BEGINNER), req("CI/CD", false, SkillLevel.BEGINNER),
                req("Microsserviços", false, SkillLevel.BEGINNER));

        // --- 7) Baixa aderência (stack diferente) ---
        job("Desenvolvedor Python Júnior", "Cais Dados", "Remoto", WorkMode.REMOTE, 4200, 5200,
                today.plusDays(25),
                "Requisitos: Python, SQL e Git. Diferencial: AWS.",
                req("Python", true, SkillLevel.INTERMEDIATE), req("SQL", true, SkillLevel.BEGINNER),
                req("Git", true, SkillLevel.BEGINNER), req("AWS", false, SkillLevel.BEGINNER));

        // --- 8) Proposta recebida ---
        job("Analista de Sistemas", "Maré Tecnologia", "Fortaleza, CE", WorkMode.HYBRID, 5500, 7000,
                today.minusDays(35),
                "Requisitos: Java, Spring Boot, SQL e Git.",
                req("Java", true, SkillLevel.INTERMEDIATE), req("Spring Boot", true, SkillLevel.BEGINNER),
                req("SQL", true, SkillLevel.INTERMEDIATE), req("Git", true, SkillLevel.BEGINNER));
        Long a8 = application(lastJobId(), ApplicationStatus.APPLIED, today.minusDays(40), null, null);
        advance(a8, ApplicationStatus.SCREENING, ApplicationStatus.INTERVIEWING, ApplicationStatus.OFFER);
        interview(a8, InterviewType.HR, today.minusDays(30).atTime(15, 0), "RH Maré", "Presencial",
                InterviewStatus.COMPLETED, "Feedback positivo.");
        interview(a8, InterviewType.TECHNICAL, today.minusDays(20).atTime(9, 30), "Tech Lead", "Presencial",
                InterviewStatus.COMPLETED, "Perguntas de POO, SQL e um exercício de código.");
        backdate(a8, today.minusDays(2));

        log.info("Dados de demonstração carregados (vagas, candidaturas, entrevistas e contatos fictícios).");
    }

    // ---------- helpers ----------

    private void skill(String name, SkillLevel level) {
        skills.create(new SkillRequest(name, level));
    }

    private static RequirementRequest req(String name, boolean mandatory, SkillLevel level) {
        return new RequirementRequest(name, mandatory, level);
    }

    private void job(String title, String company, String location, WorkMode mode, int min, int max,
                     LocalDate deadline, String description, RequirementRequest... requirements) {
        lastJobId = jobs.create(new JobRequest(title, company, location, mode, BigDecimal.valueOf(min),
                BigDecimal.valueOf(max), null, description, deadline, List.of(requirements))).id();
    }

    private Long lastJobId() {
        return lastJobId;
    }

    private Long application(Long jobId, ApplicationStatus status, LocalDate appliedAt, LocalDate followUp,
                             String notes) {
        return applications.create(new CreateApplicationRequest(jobId, status, appliedAt, followUp, notes)).id();
    }

    private void advance(Long applicationId, ApplicationStatus... path) {
        for (ApplicationStatus status : path) {
            applications.changeStatus(applicationId, new ChangeStatusRequest(status, null));
        }
    }

    /** Ajusta a data da última mudança de status (apenas para a demonstração parecer realista). */
    private void backdate(Long applicationId, LocalDate date) {
        applicationRepository.findById(applicationId).ifPresent(application -> {
            application.setStatusChangedAt(LocalDateTime.of(date, LocalTime.NOON));
            applicationRepository.save(application);
        });
    }

    private void stage(Long applicationId, StageType type, String title, StageStatus status, LocalDate due) {
        process.addStage(applicationId, new StageRequest(type, title, status, due, null));
    }

    private void interview(Long applicationId, InterviewType type, LocalDateTime at, String interviewer,
                           String location, InterviewStatus status, String feedback) {
        process.addInterview(applicationId,
                new InterviewRequest(type, at, interviewer, location, status, feedback));
    }

    private void contact(Long applicationId, String name, String role, String email, String linkedin) {
        process.addContact(applicationId, new ContactRequest(name, role, email, null, linkedin, null));
    }
}
