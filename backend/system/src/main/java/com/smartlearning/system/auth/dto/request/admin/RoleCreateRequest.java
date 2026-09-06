package com.smartlearning.system.auth.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RoleCreateRequest(

        @NotBlank(message = "Mã role không được để trống")
        @Size(max = 50,
                message = "Mã role không được vượt quá 50 ký tự")
        @Pattern(regexp = "^[A-Z_]*$",
                message = "Mã role chỉ chứa chữ in hoa, số, dấu gạch dưới")
        String code,

        @NotBlank(message = "Tên role không được để trống")
        @Size(max = 100,
                message = "Tên role không được vượt quá 100 ký tự")
        String name,

        Set<Long> permissionIds
) {
}
