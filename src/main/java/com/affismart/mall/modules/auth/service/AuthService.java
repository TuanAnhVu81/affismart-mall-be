package com.affismart.mall.modules.auth.service;

import com.affismart.mall.common.enums.RoleName;
import com.affismart.mall.common.enums.UserStatus;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.auth.dto.request.LoginRequest;
import com.affismart.mall.modules.auth.dto.request.RegisterRequest;
import com.affismart.mall.modules.auth.dto.response.AuthTokenResponse;
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
import java.util.List;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

	private static final String BEARER_TOKEN_TYPE = "Bearer";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;
	private final AuthMapper authMapper;

	public AuthService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService,
			RefreshTokenService refreshTokenService,
			AuthMapper authMapper
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.refreshTokenService = refreshTokenService;
		this.authMapper = authMapper;
	}

	@Transactional
	public AuthUserResponse register(RegisterRequest request) {
		String normalizedEmail = normalizeEmail(request.email());
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}

		Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
				.orElseThrow(() -> new AppException(ErrorCode.DEFAULT_ROLE_NOT_FOUND));

		// Map RegisterRequest to User entity via AuthMapper, then set fields that
		// require special handling (password encoding, default status)
		User user = authMapper.toUser(request);
		user.setEmail(normalizedEmail);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setStatus(UserStatus.ACTIVE);

		User savedUser = userRepository.save(user);
		UserRole userRole = userRoleRepository.save(new UserRole(savedUser, customerRole));

		// Re-attach the persisted role mapping so the mapper can extract roles
		savedUser.getUserRoles().add(userRole);
		return authMapper.toAuthUserResponse(savedUser);
	}

	@Transactional(readOnly = true)
	public AuthenticatedSession login(LoginRequest request, String ipAddress, String userAgent) {
		String normalizedEmail = normalizeEmail(request.email());

		try {
			Authentication authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, request.password())
			);

			UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
			User user = userRepository.findWithRolesById(principal.getUserId())
					.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

			RefreshTokenSession refreshTokenSession = refreshTokenService.issue(user.getId(), ipAddress, userAgent);
			return new AuthenticatedSession(buildTokenResponse(user), refreshTokenSession.token());
		} catch (DisabledException exception) {
			throw new AppException(ErrorCode.USER_NOT_ACTIVE);
		} catch (BadCredentialsException exception) {
			throw new AppException(ErrorCode.INVALID_CREDENTIALS);
		}
	}

	@Transactional(readOnly = true)
	public AuthenticatedSession refresh(String refreshToken, String ipAddress, String userAgent) {
		RefreshTokenSession refreshTokenSession = refreshTokenService.rotate(refreshToken, ipAddress, userAgent);
		User user = userRepository.findWithRolesById(refreshTokenSession.userId())
				.orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "User account is no longer available"));

		if (user.getStatus() != UserStatus.ACTIVE) {
			refreshTokenService.revokeAllSessions(user.getId());
			throw new AppException(ErrorCode.USER_NOT_ACTIVE);
		}

		return new AuthenticatedSession(buildTokenResponse(user), refreshTokenSession.token());
	}

	public void logout(String refreshToken) {
		if (StringUtils.hasText(refreshToken)) {
			refreshTokenService.revoke(refreshToken);
		}
	}

	private AuthTokenResponse buildTokenResponse(User user) {
		List<String> roles = user.getUserRoles().stream()
				.map(userRole -> userRole.getRole().getName().name())
				.toList();

		String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roles);
		return new AuthTokenResponse(
				accessToken,
				BEARER_TOKEN_TYPE,
				jwtService.extractExpiration(accessToken),
				authMapper.toAuthUserResponse(user)
		);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
