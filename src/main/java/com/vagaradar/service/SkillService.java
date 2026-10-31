package com.vagaradar.service;

import com.vagaradar.analysis.SkillCatalog;
import com.vagaradar.domain.UserSkill;
import com.vagaradar.repository.UserSkillRepository;
import com.vagaradar.web.dto.SkillDtos.SkillRequest;
import com.vagaradar.web.dto.SkillDtos.SkillResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SkillService {

    private final UserSkillRepository skills;

    public SkillService(UserSkillRepository skills) {
        this.skills = skills;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> list() {
        return skills.findAllByOrderByNameAsc().stream().map(SkillResponse::from).toList();
    }

    @Transactional
    public SkillResponse create(SkillRequest request) {
        String key = SkillCatalog.key(request.name());
        if (skills.findByNormalizedName(key).isPresent()) {
            throw new BusinessRuleException("A competência '" + SkillCatalog.canonicalName(request.name())
                    + "' já está cadastrada. Edite o nível dela em vez de criar outra.");
        }
        UserSkill skill = skills.save(new UserSkill(SkillCatalog.canonicalName(request.name()), key, request.level()));
        return SkillResponse.from(skill);
    }

    @Transactional
    public SkillResponse update(Long id, SkillRequest request) {
        UserSkill skill = find(id);
        String key = SkillCatalog.key(request.name());
        skills.findByNormalizedName(key).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new BusinessRuleException("Já existe outra competência com o nome '" + other.getName() + "'.");
            }
        });
        skill.setName(SkillCatalog.canonicalName(request.name()));
        skill.setNormalizedName(key);
        skill.setLevel(request.level());
        return SkillResponse.from(skill);
    }

    @Transactional
    public void delete(Long id) {
        skills.delete(find(id));
    }

    private UserSkill find(Long id) {
        return skills.findById(id).orElseThrow(() -> new NotFoundException("Competência não encontrada: " + id));
    }
}
