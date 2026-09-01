package com.smartlearning.system.auth.service.impl;

import com.smartlearning.system.auth.dto.response.PermissionResponse;
import com.smartlearning.system.auth.entity.Permission;
import com.smartlearning.system.auth.repository.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.smartlearning.system.support.SystemTestData.COURSE_READ_PERMISSION_ID;
import static com.smartlearning.system.support.SystemTestData.COURSE_WRITE_PERMISSION_ID;
import static com.smartlearning.system.support.SystemTestData.permission;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;
    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    void getPermissions_mapsRepositoryOrderToResponses() {
        Permission read = permission(COURSE_READ_PERMISSION_ID, "COURSE_READ");
        Permission write = permission(COURSE_WRITE_PERMISSION_ID, "COURSE_WRITE");
        when(permissionRepository.findAllByOrderByCodeAsc())
                .thenReturn(List.of(read, write));

        List<PermissionResponse> result = permissionService.getPermissions();

        assertThat(result).extracting(PermissionResponse::code)
                .containsExactly("COURSE_READ", "COURSE_WRITE");
        assertThat(result).extracting(PermissionResponse::id)
                .containsExactly(COURSE_READ_PERMISSION_ID, COURSE_WRITE_PERMISSION_ID);
    }
}
