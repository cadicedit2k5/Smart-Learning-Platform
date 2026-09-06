package com.smartlearning.system.auth.entity;

import com.smartlearning.common.entity.BaseEntity;
import com.smartlearning.common.entity.BaseEntityLong;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "permission", schema = "system")
public class Permission extends BaseEntityLong {
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String description;
}
