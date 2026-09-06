package com.smartlearning.system.auth.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RoleUpdateRequest(

        @NotBlank(message = "Tên role không được để trống")
        @Size(max = 100, message = "Tên role không được vượt quá 100 ký tự")
        String name,

        @NotNull(message = "Danh sách permission không được null")
        Set<@NotNull Long> permissionIds
) {
}
