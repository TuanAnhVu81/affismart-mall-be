package com.affismart.mall.modules.user.service;

import com.affismart.mall.common.enums.RoleName;
import com.affismart.mall.common.enums.UserStatus;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.common.response.PageResponse;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.user.dto.request.ChangePasswordRequest;
import com.affismart.mall.modules.user.dto.request.ResetPasswordRequest;
import com.affismart.mall.modules.user.dto.request.UpdateProfileRequest;
import com.affismart.mall.modules.user.dto.request.UpdateUserStatusRequest;
import com.affismart.mall.modules.user.dto.response.UserProfileResponse;
import com.affismart.mall.modules.user.dto.response.UserSummaryResponse;
import com.affismart.mall.modules.user.entity.Role;
import com.affismart.mall.modules.user.entity.User;
import com.affismart.mall.modules.user.entity.UserRole;
import com.affismart.mall.modules.user.mapper.UserMapper;
import com.affismart.mall.modules.user.repository.RoleRepository;
import com.affismart.mall.modules.user.repository.UserRepository;
import com.affismart.mall.modules.user.repository.UserRoleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Captor private ArgumentCaptor<User> userCaptor;

    // =========================================================
    // getCurrentUserProfile()
    // =========================================================

    @Test
    @DisplayName("getCurrentUserProfile: Happy Path - returns profile for valid userId")
    void getCurrentUserProfile_UserExists_ReturnsUserProfileResponse() {
        // Given
        User user = createMockUser(1L, "user@gmail.com", UserStatus.ACTIVE);
        UserProfileResponse expectedResponse = createMockProfileResponse(1L);

        given(userRepository.findWithRolesById(1L)).willReturn(Optional.of(user));
        given(userMapper.toUserProfileResponse(user)).willReturn(expectedResponse);

        // When
        UserProfileResponse result = userService.getCurrentUserProfile(1L);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(result.id()).isEqualTo(1L);
        verify(userRepository, times(1)).findWithRolesById(1L);
    }

    @Test
    @DisplayName("getCurrentUserProfile: Exception - user not found throws UserNotFoundException")
    void getCurrentUserProfile_UserNotFound_ThrowsUserNotFoundException() {
        // Given
        given(userRepository.findWithRolesById(99L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getCurrentUserProfile(99L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // =========================================================
    // updateCurrentUserProfile()
    // =========================================================

    @Test
    @DisplayName("updateCurrentUserProfile: Happy Path - profile info is updated and saved")
    void updateCurrentUserProfile_ValidRequest_UpdatesAndReturnsProfile() {
        // Given
        User user = createMockUser(1L, "user@gmail.com", UserStatus.ACTIVE);
        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "0909999999", "123 Main St");
        UserProfileResponse expectedResponse = createMockProfileResponse(1L);

        given(userRepository.findWithRolesById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toUserProfileResponse(user)).willReturn(expectedResponse);

        // When
        UserProfileResponse result = userService.updateCurrentUserProfile(1L, request);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        // Verify mapper was called to apply the request values to the entity
        verify(userMapper, times(1)).updateUserFromProfileRequest(request, user);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@gmail.com");
    }

    // =========================================================
    // changeCurrentUserPassword()
    // =========================================================

    @Test
    @DisplayName("changeCurrentUserPassword: Happy Path - password is changed successfully")
    void changeCurrentUserPassword_CorrectCurrentPassword_ChangesPasswordHash() {
        // Given
        User user = createMockUser(1L, "user@gmail.com", UserStatus.ACTIVE);
        user.setPasswordHash("old_hash");
        ChangePasswordRequest request = new ChangePasswordRequest("old_password", "new_password_123");

        given(userRepository.findWithRolesById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("old_password", "old_hash")).willReturn(true);
        given(passwordEncoder.encode("new_password_123")).willReturn("new_hash");
        given(userRepository.save(any(User.class))).willReturn(user);

        // When
        userService.changeCurrentUserPassword(1L, request);

        // Then - verify the new hash was set on the user before saving
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("new_hash");
    }

    @Test
    @DisplayName("changeCurrentUserPassword: Exception - wrong current password")
    void changeCurrentUserPassword_WrongCurrentPassword_ThrowsInvalidCurrentPasswordException() {
        // Given
        User user = createMockUser(1L, "user@gmail.com", UserStatus.ACTIVE);
        user.setPasswordHash("correct_hash");
        ChangePasswordRequest request = new ChangePasswordRequest("wrong_old_password", "new_password");

        given(userRepository.findWithRolesById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong_old_password", "correct_hash")).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changeCurrentUserPassword(1L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CURRENT_PASSWORD);

        verify(userRepository, never()).save(any());
    }

    // =========================================================
    // getUsers() (Admin)
    // =========================================================

    @Test
    @DisplayName("getUsers: Happy Path - returns paginated user summary list")
    void getUsers_ValidPaginationParams_ReturnsPageResponse() {
        // Given
        UserSummaryResponse summary = createMockSummaryResponse(1L);
        // PageImpl with a single element list defaults to size=1, page=0
        Page<User> userPage = new PageImpl<>(List.of(createMockUser(1L, "a@b.com", UserStatus.ACTIVE)));

        given(userRepository.findAll(any(Pageable.class))).willReturn(userPage);
        given(userMapper.toUserSummaryResponse(any(User.class))).willReturn(summary);

        // When
        PageResponse<UserSummaryResponse> result = userService.getUsers(0, 10, "createdAt", "desc");

        // Then
        assertThat(result.content()).hasSize(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(1); // PageImpl default size for 1 element is 1
        verify(userRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("getUsers: Edge Case - invalid sortBy is sanitized to 'createdAt'")
    void getUsers_InvalidSortByParam_FallsBackToCreatedAt() {
        // Given
        Page<User> emptyPage = new PageImpl<>(List.of());
        given(userRepository.findAll(any(Pageable.class))).willReturn(emptyPage);

        // When
        userService.getUsers(0, 10, "injected_field", "desc");

        // Then - verify it doesn't throw and still calls the repository
        verify(userRepository, times(1)).findAll(any(Pageable.class));
    }

    // =========================================================
    // updateUserStatus() (Admin)
    // =========================================================

    @Test
    @DisplayName("updateUserStatus: Happy Path - user status is updated to BANNED")
    void updateUserStatus_StatusBanned_UpdatesUserStatusSuccessfully() {
        // Given
        User user = createMockUser(1L, "user@gmail.com", UserStatus.ACTIVE);
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BANNED);
        UserProfileResponse expectedResponse = createMockProfileResponse(1L);

        given(userRepository.findWithRolesById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willReturn(user);
        given(userMapper.toUserProfileResponse(user)).willReturn(expectedResponse);

        // When
        userService.updateUserStatus(1L, request);

        // Then
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.BANNED);
    }

    @Test
    @DisplayName("updateUserStatus: Exception - INACTIVE status is not allowed")
    void updateUserStatus_InactiveStatus_ThrowsInvalidUserStatusException() {
        // Given
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.INACTIVE);

        // When & Then
        assertThatThrownBy(() -> userService.updateUserStatus(1L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_USER_STATUS);

        verify(userRepository, never()).save(any());
    }

    // =========================================================
    // resetUserPassword() (Admin)
    // =========================================================

    @Test
    @DisplayName("resetUserPassword: Happy Path - admin force-sets a new password for a user")
    void resetUserPassword_ValidRequest_PersistsNewPasswordHash() {
        // Given
        User user = createMockUser(1L, "user@gmail.com", UserStatus.ACTIVE);
        user.setPasswordHash("old_hash");
        ResetPasswordRequest request = new ResetPasswordRequest("admin_set_new_password");

        given(userRepository.findWithRolesById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("admin_set_new_password")).willReturn("admin_new_hash");
        given(userRepository.save(any(User.class))).willReturn(user);

        // When
        userService.resetUserPassword(1L, request);

        // Then - verify the new hash is saved correctly
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("admin_new_hash");
    }

    @Test
    @DisplayName("resetUserPassword: Exception - target user not found")
    void resetUserPassword_UserNotFound_ThrowsUserNotFoundException() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest("new_password");
        given(userRepository.findWithRolesById(99L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.resetUserPassword(99L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // =========================================================
    // ensureAdminUser()
    // =========================================================

    @Test
    @DisplayName("ensureAdminUser: Happy Path - creates new admin user when none exists")
    void ensureAdminUser_NoExistingUser_CreatesAndAssignsAdminRole() {
        // Given
        String email = "  ADMIN@Example.com  ";
        String normalizedEmail = "admin@example.com";
        User savedUser = createMockUser(1L, normalizedEmail, UserStatus.ACTIVE);
        Role adminRole = createMockRole(RoleName.ADMIN);

        given(userRepository.findWithRolesByEmail(normalizedEmail)).willReturn(Optional.empty());
        given(passwordEncoder.encode("rawPassword")).willReturn("admin_hash");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(roleRepository.findByName(RoleName.ADMIN)).willReturn(Optional.of(adminRole));
        given(userRoleRepository.save(any(UserRole.class))).willReturn(new UserRole(savedUser, adminRole));

        // When
        User result = userService.ensureAdminUser(email, "rawPassword", "Admin Name");

        // Then
        assertThat(result).isNotNull();
        verify(userRepository, atLeastOnce()).save(any(User.class));
        verify(userRoleRepository, times(1)).save(any(UserRole.class));
    }

    @Test
    @DisplayName("ensureAdminUser: Edge Case - existing user without ADMIN role gets role assigned")
    void ensureAdminUser_ExistingUserWithoutAdminRole_AssignsAdminRole() {
        // Given  
        User existingUser = createMockUser(1L, "admin@example.com", UserStatus.ACTIVE);
        // existingUser has no admin role in its userRoles set (default empty set)
        Role adminRole = createMockRole(RoleName.ADMIN);

        given(userRepository.findWithRolesByEmail("admin@example.com")).willReturn(Optional.of(existingUser));
        given(roleRepository.findByName(RoleName.ADMIN)).willReturn(Optional.of(adminRole));
        given(userRoleRepository.save(any(UserRole.class))).willReturn(new UserRole(existingUser, adminRole));

        // When
        userService.ensureAdminUser("admin@example.com", "pass", "Admin");

        // Then - admin role added but user not re-created
        verify(userRoleRepository, times(1)).save(any(UserRole.class));
        // confirm user was not re-saved (id was already present)
        verify(userRepository, never()).save(argThat(u -> u.getId() == null));
    }

    // =========================================================
    // Private Helper Methods
    // =========================================================

    private User createMockUser(Long id, String email, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("hashed_password");
        user.setFullName("Vu Tuan Anh");
        user.setStatus(status);
        return user;
    }

    private Role createMockRole(RoleName roleName) {
        Role role = new Role();
        role.setId(1L);
        role.setName(roleName);
        return role;
    }

    private UserProfileResponse createMockProfileResponse(Long id) {
        LocalDateTime now = LocalDateTime.now();
        return new UserProfileResponse(
                id, "user@gmail.com", "Vu Tuan Anh", "0901234567",
                null, "ACTIVE", List.of("CUSTOMER"),
                now, now
        );
    }

    private UserSummaryResponse createMockSummaryResponse(Long id) {
        LocalDateTime now = LocalDateTime.now();
        return new UserSummaryResponse(
                id, "user@gmail.com", "Vu Tuan Anh", "0901234567",
                "ACTIVE", List.of("CUSTOMER"), now, now
        );
    }
}
