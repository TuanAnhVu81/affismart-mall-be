package com.affismart.mall.modules.auth.service;

import com.affismart.mall.common.enums.RoleName;
import com.affismart.mall.common.enums.UserStatus;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.auth.dto.request.LoginRequest;
import com.affismart.mall.modules.auth.dto.request.RegisterRequest;
import com.affismart.mall.modules.auth.dto.response.AuthUserResponse;
import com.affismart.mall.modules.auth.mapper.AuthMapper;
import com.affismart.mall.modules.auth.model.AuthenticatedSession;
import com.affismart.mall.modules.auth.model.RefreshTokenSession;
import com.affismart.mall.modules.auth.security.UserPrincipal;
import com.affismart.mall.modules.user.entity.Role;
import com.affismart.mall.modules.user.entity.User;
import com.affismart.mall.modules.user.entity.UserRole;
import com.affismart.mall.modules.user.repository.RoleRepository;
import com.affismart.mall.modules.user.repository.UserRepository;
import com.affismart.mall.modules.user.repository.UserRoleRepository;
import java.time.Instant;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuthMapper authMapper;

    @InjectMocks
    private AuthService authService;

    @Captor private ArgumentCaptor<User> userCaptor;

    // =========================================================
    // register()
    // =========================================================

    @Test
    @DisplayName("register: Happy Path - new user registered successfully")
    void register_ValidRequest_ReturnsAuthUserResponse() {
        // Given
        RegisterRequest request = createRegisterRequest("  TEST@Gmail.COM  ");
        User mappedUser = new User();
        User savedUser = createMockUser(1L, "test@gmail.com", UserStatus.ACTIVE);
        Role customerRole = createMockRole(RoleName.CUSTOMER);
        UserRole savedUserRole = new UserRole(savedUser, customerRole);
        AuthUserResponse expectedResponse = createAuthUserResponse();

        given(userRepository.existsByEmail("test@gmail.com")).willReturn(false);
        given(roleRepository.findByName(RoleName.CUSTOMER)).willReturn(Optional.of(customerRole));
        given(authMapper.toUser(request)).willReturn(mappedUser);
        given(passwordEncoder.encode(request.password())).willReturn("hashed_password");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(userRoleRepository.save(any(UserRole.class))).willReturn(savedUserRole);
        given(authMapper.toAuthUserResponse(savedUser)).willReturn(expectedResponse);

        // When
        AuthUserResponse result = authService.register(request);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(userRepository, times(1)).existsByEmail("test@gmail.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userRoleRepository, times(1)).save(any(UserRole.class));
    }

    @Test
    @DisplayName("register: email is normalized (trim + lowercase) before checking uniqueness")
    void register_EmailWithSpacesAndUppercase_IsNormalizedBeforeSave() {
        // Given
        RegisterRequest request = createRegisterRequest("  UPPER@Example.com  ");
        User mappedUser = new User();
        User savedUser = createMockUser(1L, "upper@example.com", UserStatus.ACTIVE);
        Role role = createMockRole(RoleName.CUSTOMER);

        given(userRepository.existsByEmail("upper@example.com")).willReturn(false);
        given(roleRepository.findByName(RoleName.CUSTOMER)).willReturn(Optional.of(role));
        given(authMapper.toUser(request)).willReturn(mappedUser);
        given(passwordEncoder.encode(any())).willReturn("hashed");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(userRoleRepository.save(any(UserRole.class))).willReturn(new UserRole());
        given(authMapper.toAuthUserResponse(savedUser)).willReturn(createAuthUserResponse());

        // When
        authService.register(request);

        // Then - verify the normalized email was used for the duplicate check
        verify(userRepository).existsByEmail("upper@example.com");
    }

    @Test
    @DisplayName("register: Exception - email already exists")
    void register_EmailAlreadyExists_ThrowsEmailAlreadyExistsException() {
        // Given
        RegisterRequest request = createRegisterRequest("existing@gmail.com");
        given(userRepository.existsByEmail("existing@gmail.com")).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: Exception - default CUSTOMER role not found in DB")
    void register_CustomerRoleNotFound_ThrowsDefaultRoleNotFoundException() {
        // Given
        RegisterRequest request = createRegisterRequest("new@gmail.com");
        given(userRepository.existsByEmail("new@gmail.com")).willReturn(false);
        given(roleRepository.findByName(RoleName.CUSTOMER)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEFAULT_ROLE_NOT_FOUND);
    }

    // =========================================================
    // login()
    // =========================================================

    @Test
    @DisplayName("login: Happy Path - valid credentials return session with access and refresh token")
    void login_ValidCredentials_ReturnsAuthenticatedSession() {
        // Given
        LoginRequest request = new LoginRequest("user@gmail.com", "password123");
        UserPrincipal principal = createMockPrincipal(1L, "user@gmail.com");
        Authentication authentication = mock(Authentication.class);
        RefreshTokenSession session = new RefreshTokenSession("refresh-uuid", 1L);

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(principal);
        given(refreshTokenService.issue(1L)).willReturn(session);
        given(jwtService.generateAccessToken(eq(1L), eq("user@gmail.com"), any())).willReturn("access-token");
        given(jwtService.extractExpiration("access-token")).willReturn(Instant.now());

        // When
        AuthenticatedSession result = authService.login(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.refreshToken()).isEqualTo("refresh-uuid");
        assertThat(result.tokenResponse().accessToken()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("login: Exception - wrong password throws InvalidCredentialsException")
    void login_WrongPassword_ThrowsInvalidCredentialsException() {
        // Given
        LoginRequest request = new LoginRequest("user@gmail.com", "wrong-password");
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("login: Exception - disabled/inactive user throws UserNotActiveException")
    void login_DisabledUser_ThrowsUserNotActiveException() {
        // Given
        LoginRequest request = new LoginRequest("banned@gmail.com", "password123");
        given(authenticationManager.authenticate(any()))
                .willThrow(new DisabledException("User is disabled"));

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_ACTIVE);
    }

    // =========================================================
    // refresh()
    // =========================================================

    @Test
    @DisplayName("refresh: Happy Path - valid token rotated and new session returned")
    void refresh_ValidToken_RotatesAndReturnsNewSession() {
        // Given
        String oldToken = "old-refresh-token";
        RefreshTokenSession rotatedSession = new RefreshTokenSession("new-refresh-uuid", 1L);
        User activeUser = createMockUser(1L, "user@gmail.com", UserStatus.ACTIVE);

        given(refreshTokenService.rotate(oldToken)).willReturn(rotatedSession);
        given(userRepository.findWithRolesById(1L)).willReturn(Optional.of(activeUser));
        given(jwtService.generateAccessToken(any(), any(), any())).willReturn("new-access-token");
        given(jwtService.extractExpiration("new-access-token")).willReturn(Instant.now());

        // When
        AuthenticatedSession result = authService.refresh(oldToken);

        // Then
        assertThat(result.refreshToken()).isEqualTo("new-refresh-uuid");
        verify(refreshTokenService, times(1)).rotate(oldToken);
        verify(refreshTokenService, never()).revokeAllSessions(any());
    }

    @Test
    @DisplayName("refresh: Exception - user is banned, all sessions are revoked")
    void refresh_BannedUser_RevokesAllSessionsAndThrowsUserNotActiveException() {
        // Given
        String token = "refresh-token";
        RefreshTokenSession session = new RefreshTokenSession("new-token", 1L);
        User bannedUser = createMockUser(1L, "banned@gmail.com", UserStatus.BANNED);

        given(refreshTokenService.rotate(token)).willReturn(session);
        given(userRepository.findWithRolesById(1L)).willReturn(Optional.of(bannedUser));

        // When & Then
        assertThatThrownBy(() -> authService.refresh(token))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_ACTIVE);

        // Verify all sessions are revoked as a security measure
        verify(refreshTokenService, times(1)).revokeAllSessions(1L);
    }

    @Test
    @DisplayName("refresh: Exception - user not found in DB")
    void refresh_UserNotFound_ThrowsUnauthorizedException() {
        // Given
        String token = "refresh-token";
        RefreshTokenSession session = new RefreshTokenSession("new-token", 99L);

        given(refreshTokenService.rotate(token)).willReturn(session);
        given(userRepository.findWithRolesById(99L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refresh(token))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    // =========================================================
    // logout()
    // =========================================================

    @Test
    @DisplayName("logout: Happy Path - valid token is revoked")
    void logout_ValidRefreshToken_RevokesToken() {
        // Given
        String refreshToken = "valid-refresh-token";

        // When
        authService.logout(refreshToken);

        // Then
        verify(refreshTokenService, times(1)).revoke(refreshToken);
    }

    @Test
    @DisplayName("logout: Edge Case - null token does nothing gracefully")
    void logout_NullToken_DoesNotCallRevoke() {
        // Given
        String refreshToken = null;

        // When
        authService.logout(refreshToken);

        // Then
        verify(refreshTokenService, never()).revoke(any());
    }

    @Test
    @DisplayName("logout: Edge Case - blank token does nothing gracefully")
    void logout_BlankToken_DoesNotCallRevoke() {
        // Given
        String refreshToken = "   ";

        // When
        authService.logout(refreshToken);

        // Then
        verify(refreshTokenService, never()).revoke(any());
    }

    // =========================================================
    // Private Helper Methods
    // =========================================================

    private RegisterRequest createRegisterRequest(String email) {
        return new RegisterRequest(email, "password123", "Vu Tuan Anh", "0901234567");
    }

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

    private UserPrincipal createMockPrincipal(Long userId, String email) {
        return new UserPrincipal(
                userId,
                email,
                "hashed_password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                true,
                true
        );
    }

    private AuthUserResponse createAuthUserResponse() {
        return new AuthUserResponse(1L, "test@gmail.com", "Vu Tuan Anh", "ACTIVE", List.of("CUSTOMER"));
    }

}
