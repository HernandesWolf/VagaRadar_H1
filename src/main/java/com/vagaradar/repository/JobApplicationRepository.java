package com.vagaradar.repository;

import com.vagaradar.domain.ApplicationStatus;
import com.vagaradar.domain.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Optional<JobApplication> findByJobId(Long jobId);

    boolean existsByJobId(Long jobId);

    List<JobApplication> findByJobIdIn(Collection<Long> jobIds);

    @Query("select a from JobApplication a join fetch a.job order by a.statusChangedAt desc")
    List<JobApplication> findAllWithJob();

    @Query("select a from JobApplication a join fetch a.job where a.status = :status order by a.statusChangedAt desc")
    List<JobApplication> findByStatusWithJob(ApplicationStatus status);

    List<JobApplication> findByFollowUpDateLessThanEqual(LocalDate date);
}
