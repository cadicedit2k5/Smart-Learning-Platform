package com.smartlearning.system.auth.service.impl;

import com.smartlearning.system.auth.dto.response.PermissionResponse;
import com.smartlearning.system.auth.entity.Permission;
import com.smartlearning.system.auth.repository.PermissionRepository;
import com.smartlearning.system.auth.service.PermissionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;

    @Override
    public List<PermissionResponse> getPermissions() {
        return permissionRepository.findAllByOrderByCodeAsc().stream()
                .map(PermissionResponse::from).toList();
    }
}
