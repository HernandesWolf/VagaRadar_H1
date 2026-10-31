package com.vagaradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Requisito (tecnologia/competência) de uma vaga. */
@Entity
@Table(name = "job_requirements")
public class JobRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobOpportunity job;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 80)
    private String normalizedName;

    @Column(nullable = false)
    private boolean mandatory;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_level", nullable = false, length = 20)
    private SkillLevel minLevel;

    protected JobRequirement() {
    }

    public JobRequirement(JobOpportunity job, String name, String normalizedName,
                          boolean mandatory, SkillLevel minLevel) {
        this.job = job;
        this.name = name;
        this.normalizedName = normalizedName;
        this.mandatory = mandatory;
        this.minLevel = minLevel;
    }

    public Long getId() { return id; }
    public JobOpportunity getJob() { return job; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNormalizedName() { return normalizedName; }
    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
    public SkillLevel getMinLevel() { return minLevel; }
    public void setMinLevel(SkillLevel minLevel) { this.minLevel = minLevel; }
}
