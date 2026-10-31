package com.vagaradar.web.dto;

import com.vagaradar.domain.SkillLevel;
import com.vagaradar.domain.UserSkill;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SkillDtos {

    private SkillDtos() {
    }

    public record SkillRequest(@NotBlank @Size(max = 80) String name, @NotNull SkillLevel level) {
    }

    public record SkillResponse(Long id, String name, SkillLevel level) {
        public static SkillResponse from(UserSkill skill) {
            return new SkillResponse(skill.getId(), skill.getName(), skill.getLevel());
        }
    }
}
