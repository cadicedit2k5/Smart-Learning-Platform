package com.smartlearning.system.auth.entity;

import com.smartlearning.common.entity.BaseEntityLong;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles", schema = "system")
@Getter
@Setter
public class Role extends BaseEntityLong {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            schema = "system",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns =
            @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions =
            new HashSet<>();
}