package com.smartlearning.system.auth.dto.request.admin;

import com.smartlearning.system.auth.dto.request.UserCreateRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserCreateRequest extends UserCreateRequest {
    @NotBlank(message = "Role không được để trống")
    private String roleCode;
}