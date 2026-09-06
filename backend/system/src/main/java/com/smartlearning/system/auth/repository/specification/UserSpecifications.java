package com.smartlearning.system.auth.repository.specification;

import com.smartlearning.system.auth.entity.User;
import com.smartlearning.system.auth.entity.enums.UserStatus;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;


@NoArgsConstructor
public class UserSpecifications {

    public static Specification<User> containsKeyword(
            String keyword
    ) {
        if (!StringUtils.hasText(keyword)) {
            return Specification.unrestricted();
        }

        String pattern = "%"
                + keyword.trim().toLowerCase(Locale.ROOT)
                + "%";

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("email")
                                ),
                                pattern
                        ),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("fullName")
                                ),
                                pattern
                        )
                );
    }

    public static Specification<User> hasRoleCode(
            String roleCode
    ) {
        if (!StringUtils.hasText(roleCode)) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.join("role").get("code"),
                        roleCode.trim().toUpperCase(Locale.ROOT)
                );
    }

    public static Specification<User> status(UserStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("status"),
                        status
                );
    }

    public static Specification<User> createdFrom(
            Instant createdFrom
    ) {
        if (createdFrom == null) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        createdFrom
                );
    }

    public static Specification<User> createdTo(
            Instant createdTo
    ) {
        if (createdTo == null) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"),
                        createdTo
                );
    }

}
