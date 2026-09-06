package com.smartlearning.system.auth.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.system.auth.dto.request.admin.RoleUpdateRequest;
import com.smartlearning.system.auth.dto.response.RoleDetailResponse;
import com.smartlearning.system.auth.entity.Permission;
import com.smartlearning.system.auth.entity.Role;
import com.smartlearning.system.auth.error.AuthErrorCode;
import com.smartlearning.system.auth.repository.PermissionRepository;
import com.smartlearning.system.auth.repository.RoleRepository;
import com.smartlearning.system.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.smartlearning.system.support.SystemTestData.ADMIN_ROLE_ID;
import static com.smartlearning.system.support.SystemTestData.COURSE_READ_PERMISSION_ID;
import static com.smartlearning.system.support.SystemTestData.COURSE_WRITE_PERMISSION_ID;
import static com.smartlearning.system.support.SystemTestData.STUDENT_ROLE_ID;
import static com.smartlearning.system.support.SystemTestData.permission;
import static com.smartlearning.system.support.SystemTestData.role;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void handleGetRoleByCode_returnsInitializedRole() {
        Role student = role(STUDENT_ROLE_ID, "STUDENT");
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(student));

        assertThat(roleService.handleGetRoleByCode("STUDENT")).isSameAs(student);
    }

    @Test
    void handleGetRoleByCode_rejectsUninitializedRole() {
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.handleGetRoleByCode("STUDENT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STUDENT");
    }

    @Test
    void getRoles_sortsRolesAndTheirPermissionsByCode() {
        Permission read = permission(COURSE_READ_PERMISSION_ID, "COURSE_READ");
        Permission write = permission(COURSE_WRITE_PERMISSION_ID, "COURSE_WRITE");
        Role student = role(STUDENT_ROLE_ID, "STUDENT", write, read);
        Role admin = role(ADMIN_ROLE_ID, "ADMIN", write, read);
        when(roleRepository.findAllWithPermissions()).thenReturn(List.of(student, admin));

        List<RoleDetailResponse> result = roleService.getRoles();

        assertThat(result).extracting(RoleDetailResponse::code)
                .containsExactly("ADMIN", "STUDENT");
        assertThat(result.get(0).permissions())
                .extracting(permissionResponse -> permissionResponse.code())
                .containsExactly("COURSE_READ", "COURSE_WRITE");
    }

    @Test
    void getRole_returnsNotFoundForUnknownId() {
        when(roleRepository.findByIdWithPermissions(STUDENT_ROLE_ID))
                .thenReturn(Optional.empty());

        assertError(
                () -> roleService.getRole(STUDENT_ROLE_ID),
                AuthErrorCode.ROLE_NOT_FOUND
        );
    }

    @Test
    void updateRole_trimsNameAndReplacesPermissions() {
        Permission read = permission(COURSE_READ_PERMISSION_ID, "COURSE_READ");
        Permission write = permission(COURSE_WRITE_PERMISSION_ID, "COURSE_WRITE");
        Role student = role(STUDENT_ROLE_ID, "STUDENT");
        RoleUpdateRequest request = new RoleUpdateRequest(
                "  Learner  ",
                Set.of(COURSE_READ_PERMISSION_ID, COURSE_WRITE_PERMISSION_ID)
        );
        when(roleRepository.findByIdWithPermissions(STUDENT_ROLE_ID))
                .thenReturn(Optional.of(student));
        when(permissionRepository.findAllById(request.permissionIds()))
                .thenReturn(List.of(write, read));

        RoleDetailResponse result = roleService.updateRole(STUDENT_ROLE_ID, request);

        assertThat(result.name()).isEqualTo("Learner");
        assertThat(result.permissions()).extracting(value -> value.code())
                .containsExactly("COURSE_READ", "COURSE_WRITE");
        assertThat(student.getPermissions()).containsExactlyInAnyOrder(read, write);
    }

    @ParameterizedTest(name = "permission ids {0} clear all permissions")
    @MethodSource("emptyPermissionSelections")
    void updateRole_acceptsEmptyPermissionSelection(Set<Long> permissionIds) {
        Role student = role(
                STUDENT_ROLE_ID,
                "STUDENT",
                permission(COURSE_READ_PERMISSION_ID, "COURSE_READ")
        );
        when(roleRepository.findByIdWithPermissions(STUDENT_ROLE_ID))
                .thenReturn(Optional.of(student));

        RoleDetailResponse result = roleService.updateRole(
                STUDENT_ROLE_ID,
                new RoleUpdateRequest("Learner", permissionIds)
        );

        assertThat(result.permissions()).isEmpty();
        assertThat(student.getPermissions()).isEmpty();
        verifyNoInteractions(permissionRepository);
    }

    static Stream<Arguments> emptyPermissionSelections() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(Set.of())
        );
    }

    @Test
    void updateRole_rejectsUnknownPermissionIds() {
        Role student = role(STUDENT_ROLE_ID, "STUDENT");
        Set<Long> requestedIds = Set.of(
                COURSE_READ_PERMISSION_ID,
                COURSE_WRITE_PERMISSION_ID
        );
        when(roleRepository.findByIdWithPermissions(STUDENT_ROLE_ID))
                .thenReturn(Optional.of(student));
        when(permissionRepository.findAllById(requestedIds)).thenReturn(List.of(
                permission(COURSE_READ_PERMISSION_ID, "COURSE_READ")
        ));

        assertError(
                () -> roleService.updateRole(
                        STUDENT_ROLE_ID,
                        new RoleUpdateRequest("Learner", requestedIds)
                ),
                AuthErrorCode.PERMISSION_NOT_FOUND
        );
    }

    @Test
    void updateRole_protectsAdminPermissions() {
        Role admin = role(ADMIN_ROLE_ID, "ADMIN");
        when(roleRepository.findByIdWithPermissions(ADMIN_ROLE_ID))
                .thenReturn(Optional.of(admin));

        assertError(
                () -> roleService.updateRole(
                        ADMIN_ROLE_ID,
                        new RoleUpdateRequest("Administrator", Set.of())
                ),
                AuthErrorCode.ADMIN_PERMISSIONS_PROTECTED
        );

        verify(permissionRepository, never()).findAllById(org.mockito.ArgumentMatchers.any());
    }

    private static void assertError(Runnable invocation, AuthErrorCode expectedCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(expectedCode);
    }
}
