package com.vagaradar.repository;

import com.vagaradar.domain.JobOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface JobOpportunityRepository
        extends JpaRepository<JobOpportunity, Long>, JpaSpecificationExecutor<JobOpportunity> {

    @Query("select distinct j from JobOpportunity j left join fetch j.requirements order by j.createdAt desc")
    List<JobOpportunity> findAllWithRequirements();

    List<JobOpportunity> findByDeadlineBetween(LocalDate from, LocalDate to);
}
