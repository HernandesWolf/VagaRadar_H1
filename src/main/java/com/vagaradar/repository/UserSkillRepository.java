package com.vagaradar.repository;

import com.vagaradar.domain.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    Optional<UserSkill> findByNormalizedName(String normalizedName);

    List<UserSkill> findAllByOrderByNameAsc();
}
