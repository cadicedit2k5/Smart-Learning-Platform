package com.smartlearning.system.auth.service.impl;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.storage.dto.FileUploadResponse;
import com.smartlearning.storage.service.FileStorageService;
import com.smartlearning.system.auth.dto.request.UserCreateRequest;
import com.smartlearning.system.auth.dto.request.UserFilterRequest;
import com.smartlearning.system.auth.dto.request.UserLoginRequest;
import com.smartlearning.system.auth.dto.request.UserUpdateRequest;
import com.smartlearning.system.auth.dto.request.admin.AdminUserCreateRequest;
import com.smartlearning.system.auth.dto.request.admin.AdminUserUpdateRequest;
import com.smartlearning.system.auth.dto.response.LoginResponse;
import com.smartlearning.system.auth.dto.response.UserResponse;
import com.smartlearning.system.auth.dto.response.UserLookupResponse;
import com.smartlearning.system.auth.entity.Role;
import com.smartlearning.system.auth.entity.User;
import com.smartlearning.system.auth.entity.enums.UserStatus;
import com.smartlearning.system.auth.mapper.UserMapper;
import com.smartlearning.system.auth.repository.UserRepository;
import com.smartlearning.system.auth.security.SecurityUser;
import com.smartlearning.system.auth.service.JwtTokenService;
import com.smartlearning.system.auth.service.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleService roleService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void handleGetUsers_returnsMappedPage() {
        UserFilterRequest filter = mock(UserFilterRequest.class);
        @SuppressWarnings("unchecked")
        Specification<User> specification = mock(Specification.class);
        Pageable pageable = PageRequest.of(0, 10);
        User firstUser = user(UUID.randomUUID(), "first@example.com");
        User secondUser = user(UUID.randomUUID(), "second@example.com");
        UserResponse firstResponse = response(firstUser);
        UserResponse secondResponse = response(secondUser);
        Page<User> page = new PageImpl<>(
                List.of(firstUser, secondUser),
                pageable,
                2
        );

        when(filter.specification()).thenReturn(specification);
        when(filter.pageable()).thenReturn(pageable);
        when(userRepository.findAll(specification, pageable)).thenReturn(page);
        when(userMapper.toResponse(firstUser)).thenReturn(firstResponse);
        when(userMapper.toResponse(secondUser)).thenReturn(secondResponse);

        PagingResponse<UserResponse> result = userService.handleGetUsers(filter);

        assertThat(result.getContent())
                .containsExactly(firstResponse, secondResponse);
        assertThat(result.getPageable().getPage()).isEqualTo(1);
        assertThat(result.getPageable().getSize()).isEqualTo(10);
        assertThat(result.getPageable().getTotalElements()).isEqualTo(2);
        assertThat(result.getPageable().getTotalPages()).isEqualTo(1);
    }

    @Test
    void handleSearchUsers_returnsSafeLookupProjection() {
        UserFilterRequest filter = mock(UserFilterRequest.class);
        @SuppressWarnings("unchecked")
        Specification<User> specification = mock(Specification.class);
        Pageable pageable = PageRequest.of(0, 10);
        User lecturer = user(UUID.randomUUID(), "lecturer@example.com");
        lecturer.setRole(role("LECTURER"));
        when(filter.specification()).thenReturn(specification);
        when(filter.pageable()).thenReturn(pageable);
        when(userRepository.findAll(specification, pageable))
                .thenReturn(new PageImpl<>(List.of(lecturer), pageable, 1));

        PagingResponse<UserLookupResponse> result = userService.handleSearchUsers(filter);

        assertThat(result.getContent()).singleElement().satisfies(response -> {
            assertThat(response.id()).isEqualTo(lecturer.getId());
            assertThat(response.email()).isEqualTo("lecturer@example.com");
            assertThat(response.role().code()).isEqualTo("LECTURER");
        });
        assertThat(result.getPageable().getPage()).isEqualTo(1);
    }

    @Test
    void handleGetUserLookup_returnsActiveUserAndRejectsMissingUser() {
        UUID id = UUID.randomUUID();
        User active = user(id, "student@example.com");
        active.setRole(role("STUDENT"));
        when(userRepository.findByIdAndStatusNot(id, UserStatus.DELETED))
                .thenReturn(Optional.of(active));

        UserLookupResponse result = userService.handleGetUserLookup(id);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.role().code()).isEqualTo("STUDENT");

        UUID missingId = UUID.randomUUID();
        when(userRepository.findByIdAndStatusNot(missingId, UserStatus.DELETED))
                .thenReturn(Optional.empty());
        assertNotFound(() -> userService.handleGetUserLookup(missingId));
    }

    @Test
    void handleAddUser_normalizesEmailAndPersistsUserWithoutAvatar() {
        UserCreateRequest request = createRequest(null);
        User mappedUser = user(UUID.randomUUID(), null);
        Role studentRole = role("STUDENT");
        UserResponse expected = response(mappedUser);

        when(userRepository.existsByEmailIgnoreCase("student@example.com"))
                .thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("plain-password"))
                .thenReturn("encoded-password");
        when(roleService.handleGetRoleByCode("STUDENT"))
                .thenReturn(studentRole);
        when(userRepository.save(mappedUser)).thenReturn(mappedUser);
        when(userMapper.toResponse(mappedUser)).thenReturn(expected);

        UserResponse result = userService.handleAddUser(request);

        assertThat(result).isSameAs(expected);
        assertThat(mappedUser.getEmail()).isEqualTo("student@example.com");
        assertThat(mappedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(mappedUser.getRole()).isSameAs(studentRole);
        verify(fileStorageService, never()).upload(any(), any());
    }

    @Test
    void handleAddUser_uploadsAvatarAndStoresObjectName() {
        MockMultipartFile avatar = new MockMultipartFile(
                "avatar",
                "avatar.png",
                "image/png",
                "content".getBytes(StandardCharsets.UTF_8)
        );
        UserCreateRequest request = createRequest(avatar);
        UUID userId = UUID.randomUUID();
        User mappedUser = user(userId, null);
        Role studentRole = role("STUDENT");
        FileUploadResponse uploadResponse = new FileUploadResponse(
                "system/users/" + userId + "/avatar/object-avatar.png",
                "avatar.png",
                "image/png",
                avatar.getSize()
        );
        UserResponse expected = response(mappedUser);

        when(userRepository.existsByEmailIgnoreCase("student@example.com"))
                .thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("plain-password"))
                .thenReturn("encoded-password");
        when(roleService.handleGetRoleByCode("STUDENT"))
                .thenReturn(studentRole);
        when(userRepository.save(mappedUser)).thenReturn(mappedUser);
        when(fileStorageService.upload(
                avatar,
                "system/users/" + userId + "/avatar"
        )).thenReturn(uploadResponse);
        when(userMapper.toResponse(mappedUser)).thenReturn(expected);

        UserResponse result = userService.handleAddUser(request);

        assertThat(result).isSameAs(expected);
        assertThat(mappedUser.getAvatar())
                .isEqualTo(uploadResponse.objectName());
        verify(fileStorageService).upload(
                avatar,
                "system/users/" + userId + "/avatar"
        );
    }

    @Test
    void handleAddUser_doesNotUploadEmptyAvatar() {
        MockMultipartFile emptyAvatar = new MockMultipartFile(
                "avatar",
                new byte[0]
        );
        UserCreateRequest request = createRequest(emptyAvatar);
        User mappedUser = user(UUID.randomUUID(), null);
        UserResponse expected = response(mappedUser);

        when(userRepository.existsByEmailIgnoreCase("student@example.com"))
                .thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("plain-password"))
                .thenReturn("encoded-password");
        when(roleService.handleGetRoleByCode("STUDENT"))
                .thenReturn(role("STUDENT"));
        when(userRepository.save(mappedUser)).thenReturn(mappedUser);
        when(userMapper.toResponse(mappedUser)).thenReturn(expected);

        assertThat(userService.handleAddUser(request)).isSameAs(expected);

        verify(fileStorageService, never()).upload(any(), any());
    }

    @Test
    void handleAddUser_throwsConflictWhenNormalizedEmailAlreadyExists() {
        UserCreateRequest request = createRequest(null);

        when(userRepository.existsByEmailIgnoreCase("student@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.handleAddUser(request))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception ->
                        ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.DATA_CONFLICT);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(
                passwordEncoder,
                roleService,
                userMapper,
                fileStorageService
        );
    }

    @Test
    void handleAddUser_assignsRequestedRoleForAdminRequest() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("lecturer@example.com");
        request.setPassword("plain-password");
        request.setFullName("Lecturer");
        request.setRoleCode(" lecturer ");

        User user = user(UUID.randomUUID(), null);
        Role lecturerRole = role("LECTURER");

        when(userRepository.existsByEmailIgnoreCase("lecturer@example.com"))
                .thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("plain-password"))
                .thenReturn("encoded-password");
        when(roleService.handleGetRoleByCode("LECTURER"))
                .thenReturn(lecturerRole);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response(user));

        userService.handleAddUser(request);

        assertThat(user.getRole()).isSameAs(lecturerRole);
        verify(roleService).handleGetRoleByCode("LECTURER");
    }

    @Test
    void handleUpdateUser_updatesFieldsAndEncodesNonBlankPassword() {
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest();
        request.setPassword("new-password");
        request.setFullName("New Name");
        User existingUser = user(userId, "student@example.com");
        UserResponse expected = response(existingUser);

        when(userRepository.findByIdAndStatusNot(
                userId,
                UserStatus.DELETED
        )).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("new-password"))
                .thenReturn("new-encoded-password");
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.toResponse(existingUser)).thenReturn(expected);

        UserResponse result = userService.handleUpdateUser(userId, request);

        assertThat(result).isSameAs(expected);
        assertThat(existingUser.getPassword())
                .isEqualTo("new-encoded-password");
        verify(userMapper).partialUpdate(request, existingUser);
        verify(userRepository).save(existingUser);
    }

    @Test
    void handleUpdateUser_keepsPasswordWhenRequestPasswordIsBlank() {
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest();
        request.setPassword("   ");
        User existingUser = user(userId, "student@example.com");
        existingUser.setPassword("current-password");
        UserResponse expected = response(existingUser);

        when(userRepository.findByIdAndStatusNot(
                userId,
                UserStatus.DELETED
        )).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.toResponse(existingUser)).thenReturn(expected);

        UserResponse result = userService.handleUpdateUser(userId, request);

        assertThat(result).isSameAs(expected);
        assertThat(existingUser.getPassword()).isEqualTo("current-password");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void handleUpdateUser_updatesRoleForAdminRequest() {
        UUID userId = UUID.randomUUID();

        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setRoleCode("lecturer");

        User user = user(userId, "student@example.com");
        Role lecturerRole = role("LECTURER");

        when(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED))
                .thenReturn(Optional.of(user));
        when(roleService.handleGetRoleByCode("LECTURER"))
                .thenReturn(lecturerRole);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response(user));

        userService.handleUpdateUser(userId, request);

        assertThat(user.getRole()).isSameAs(lecturerRole);
    }

    @Test
    void handleUpdateUser_throwsNotFoundWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest();

        when(userRepository.findByIdAndStatusNot(
                userId,
                UserStatus.DELETED
        )).thenReturn(Optional.empty());

        assertNotFound(() -> userService.handleUpdateUser(userId, request));

        verify(userMapper, never()).partialUpdate(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void handleDeleteUser_marksUserAsDeleted() {
        UUID userId = UUID.randomUUID();
        User existingUser = user(userId, "student@example.com");

        when(userRepository.findByIdAndStatusNot(
                userId,
                UserStatus.DELETED
        )).thenReturn(Optional.of(existingUser));

        userService.handleDeleteUser(userId);

        assertThat(existingUser.getStatus()).isEqualTo(UserStatus.DELETED);
        verify(userRepository, never()).save(any());
    }

    @Test
    void handleDeleteUser_throwsNotFoundWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findByIdAndStatusNot(
                userId,
                UserStatus.DELETED
        )).thenReturn(Optional.empty());

        assertNotFound(() -> userService.handleDeleteUser(userId));
    }

    @Test
    void handleGetCurrentUser_returnsMappedUser() {
        UUID userId = UUID.randomUUID();
        User existingUser = user(userId, "student@example.com");
        UserResponse expected = response(existingUser);

        when(userRepository.findByIdAndStatusNot(
                userId,
                UserStatus.DELETED
        )).thenReturn(Optional.of(existingUser));
        when(userMapper.toResponse(existingUser)).thenReturn(expected);

        UserResponse result = userService.handleGetCurrentUser(userId);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void handleGetCurrentUser_throwsNotFoundWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findByIdAndStatusNot(
                userId,
                UserStatus.DELETED
        )).thenReturn(Optional.empty());

        assertNotFound(() -> userService.handleGetCurrentUser(userId));
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void handleLogin_authenticatesNormalizedEmailAndReturnsToken() {
        UserLoginRequest request = new UserLoginRequest(
                "  Student@Example.COM ",
                "plain-password"
        );
        Authentication authentication = mock(Authentication.class);
        SecurityUser principal = new SecurityUser(
                UUID.randomUUID(),
                "student@example.com",
                "encoded-password",
                Set.of()
        );
        LoginResponse expected = new LoginResponse("access-token", 3600);

        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtTokenService.generateToken(principal)).thenReturn(expected);

        LoginResponse result = userService.handleLogin(request);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<Authentication> captor =
                ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal())
                .isEqualTo("student@example.com");
        assertThat(captor.getValue().getCredentials())
                .isEqualTo("plain-password");
    }

    @Test
    void handleLogin_mapsAuthenticationFailureToUnauthorized() {
        UserLoginRequest request = new UserLoginRequest(
                "student@example.com",
                "wrong-password"
        );

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> userService.handleLogin(request))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception ->
                        ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);

        verifyNoInteractions(jwtTokenService);
    }

    private static UserCreateRequest createRequest(
            MockMultipartFile avatar
    ) {
        UserCreateRequest request = new UserCreateRequest();
        request.setEmail("  Student@Example.COM ");
        request.setPassword("plain-password");
        request.setFullName("Student");
        request.setAvatar(avatar);
        return request;
    }

    private static User user(UUID id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName("Student");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private static Role role(String code) {
        Role role = new Role();
        role.setCode(code);
        return role;
    }

    private static UserResponse response(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private static void assertNotFound(Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception ->
                        ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
    }
}
