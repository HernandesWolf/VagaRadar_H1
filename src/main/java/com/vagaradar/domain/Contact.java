package com.vagaradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Pessoa de contato (recrutador, gestor...) ligada a uma candidatura. */
@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "job_role", length = 120)
    private String jobRole;

    @Column(length = 160)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @Column(length = 1000)
    private String notes;

    protected Contact() {
    }

    public Contact(JobApplication application) {
        this.application = application;
    }

    public Long getId() { return id; }
    public JobApplication getApplication() { return application; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getJobRole() { return jobRole; }
    public void setJobRole(String jobRole) { this.jobRole = jobRole; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
