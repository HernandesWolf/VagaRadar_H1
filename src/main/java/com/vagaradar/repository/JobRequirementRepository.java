package com.vagaradar.repository;

import com.vagaradar.domain.JobRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRequirementRepository extends JpaRepository<JobRequirement, Long> {
}
