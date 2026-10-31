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

import java.time.LocalDate;

/** Etapa do processo seletivo (triagem, teste técnico, entrevista...). */
@Entity
@Table(name = "selection_stages")
public class SelectionStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_type", nullable = false, length = 30)
    private StageType stageType;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StageStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(length = 1000)
    private String notes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected SelectionStage() {
    }

    public SelectionStage(JobApplication application) {
        this.application = application;
    }

    public Long getId() { return id; }
    public JobApplication getApplication() { return application; }
    public StageType getStageType() { return stageType; }
    public void setStageType(StageType stageType) { this.stageType = stageType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public StageStatus getStatus() { return status; }
    public void setStatus(StageStatus status) { this.status = status; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
