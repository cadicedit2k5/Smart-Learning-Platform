package com.smartlearning.system.auth.dto.request;

import com.smartlearning.common.dto.request.FilterRequest;
import com.smartlearning.system.auth.entity.User;
import com.smartlearning.system.auth.entity.enums.UserStatus;
import com.smartlearning.system.auth.repository.specification.UserSpecifications;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Setter
@Getter
public class UserFilterRequest extends FilterRequest<User> {
        private String keyword;
        private String roleCode;
        private UserStatus status;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private Instant createdFrom;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private Instant createdTo;

        @Override
        public Specification<User> specification() {
                return Specification.allOf(
                        UserSpecifications.containsKeyword(keyword),
                        UserSpecifications.hasRoleCode(roleCode),
                        UserSpecifications.status(status),
                        UserSpecifications.createdFrom(createdFrom),
                        UserSpecifications.createdTo(createdTo)
                );
        }
}
