package com.smartlearning.system.auth.dto.request.admin;

import com.smartlearning.system.auth.dto.request.UserUpdateRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserUpdateRequest extends UserUpdateRequest {
    private String roleCode;
}