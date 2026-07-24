package com.example.security_log_system.repository;

import com.example.security_log_system.dto.BlacklistSearchCondition;
import com.example.security_log_system.entity.IpBlacklist;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BlacklistSpecification {

    public static Specification<IpBlacklist> search(BlacklistSearchCondition condition){

        return (root,query,builder) -> {
            List<Predicate>predicates = new ArrayList<>();

            if(StringUtils.hasText(condition.getIp())){
                predicates.add(builder.equal(
                        root.get("ipAddress"),
                        condition.getIp().trim()
                ));
            }

            if(condition.getDangerLevel() != null){
                predicates.add(builder.equal(
                        root.get("dangerLevel"),
                        condition.getDangerLevel()
                ));
            }

          return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
