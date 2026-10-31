package com.vagaradar.repository;

import com.vagaradar.domain.Interview;
import com.vagaradar.domain.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByStatusAndScheduledAtBetween(InterviewStatus status, LocalDateTime from, LocalDateTime to);

    @Query("select distinct i.application.id from Interview i")
    List<Long> findApplicationIdsWithInterviews();
}
