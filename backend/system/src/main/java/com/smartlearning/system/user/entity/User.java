package com.smartlearning.system.user.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartlearning.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;


@Getter
@Setter
@Entity
@Table(name = "user", schema = "system")
@SoftDelete(
        strategy = SoftDeleteType.DELETED,
        columnName = "deleted"
)
public class User extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "avatar")
    private String avatar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id")
    private Role role;
}
