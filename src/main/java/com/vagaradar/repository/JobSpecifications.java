package com.vagaradar.repository;

import com.vagaradar.analysis.SkillCatalog;
import com.vagaradar.domain.JobOpportunity;
import com.vagaradar.domain.JobRequirement;
import com.vagaradar.service.JobFilter;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Monta dinamicamente a consulta de vagas conforme os filtros informados. */
public final class JobSpecifications {

    private JobSpecifications() {
    }

    public static Specification<JobOpportunity> from(JobFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(filter.q())) {
                String like = like(filter.q());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("title")), like),
                        cb.like(cb.lower(root.<String>get("company")), like)));
            }
            if (hasText(filter.location())) {
                predicates.add(cb.like(cb.lower(root.<String>get("location")), like(filter.location())));
            }
            if (filter.workMode() != null) {
                predicates.add(cb.equal(root.get("workMode"), filter.workMode()));
            }
            if (filter.minSalary() != null) {
                BigDecimal min = filter.minSalary();
                predicates.add(cb.or(
                        cb.greaterThanOrEqualTo(root.<BigDecimal>get("salaryMax"), min),
                        cb.greaterThanOrEqualTo(root.<BigDecimal>get("salaryMin"), min)));
            }
            if (filter.technology() != null) {
                for (String technology : filter.technology()) {
                    if (!hasText(technology)) {
                        continue;
                    }
                    // Todas as tecnologias informadas precisam existir entre os requisitos (AND).
                    Subquery<Long> sub = query.subquery(Long.class);
                    Root<JobRequirement> requirement = sub.from(JobRequirement.class);
                    sub.select(requirement.<Long>get("id")).where(
                            cb.equal(requirement.get("job").get("id"), root.get("id")),
                            cb.equal(requirement.<String>get("normalizedName"), SkillCatalog.key(technology)));
                    predicates.add(cb.exists(sub));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String like(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
