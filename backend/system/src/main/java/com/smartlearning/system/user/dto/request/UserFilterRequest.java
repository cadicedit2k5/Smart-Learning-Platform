package com.smartlearning.system.user.dto.request;

import com.smartlearning.common.dto.request.FilterRequest;
import com.smartlearning.system.user.entity.User;
import com.smartlearning.system.user.repository.specification.UserSpecifications;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Setter
@Getter
public class UserFilterRequest extends FilterRequest<User> {
        String keyword;

        String roleCode;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant createdFrom;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant createdTo;

        @Override
        public Specification<User> specification() {
                return  Specification.allOf(
                        UserSpecifications.containsKeyword(this.keyword),
                        UserSpecifications.hasRoleCode(this.roleCode),
                        UserSpecifications.createdFrom(this.createdFrom),
                        UserSpecifications.createdTo(this.createdTo)
                );
        }
}
