package com.smartlearning.system.auth.controller.admin;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.system.auth.dto.request.admin.RoleCreateRequest;
import com.smartlearning.system.auth.dto.request.admin.RoleUpdateRequest;
import com.smartlearning.system.auth.dto.response.RoleDetailResponse;
import com.smartlearning.system.auth.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
@PreAuthorize(Authorities.ROLE_MANAGE)
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDetailResponse>>> getRoles() {

        return ApiResponses.ok(roleService.getRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> getRole(
            @PathVariable Long id) {
        return ApiResponses.ok(roleService.getRole(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleDetailResponse>> createRole(
            @Valid @RequestBody RoleCreateRequest request) {
        return ApiResponses.created(roleService.createRole(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> updateRole(
            @PathVariable Long id,
            @Valid
            @RequestBody RoleUpdateRequest request) {
        return ApiResponses.ok(roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {

        roleService.deleteRole(id);

        return ApiResponses.noContent();
    }
}
