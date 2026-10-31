package com.vagaradar.repository;

import com.vagaradar.domain.SelectionStage;
import com.vagaradar.domain.StageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface SelectionStageRepository extends JpaRepository<SelectionStage, Long> {

    List<SelectionStage> findByStatusInAndDueDateLessThanEqual(Collection<StageStatus> statuses, LocalDate date);
}
