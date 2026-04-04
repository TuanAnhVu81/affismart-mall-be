package com.affismart.mall.modules.user.service;

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
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;

	public UserService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository,
			PasswordEncoder passwordEncoder,
			UserMapper userMapper
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
		this.passwordEncoder = passwordEncoder;
		this.userMapper = userMapper;
	}

	@Transactional(readOnly = true)
	public UserProfileResponse getCurrentUserProfile(Long userId) {
		return userMapper.toUserProfileResponse(getRequiredUserWithRoles(userId));
	}

	@Transactional
	public UserProfileResponse updateCurrentUserProfile(Long userId, UpdateProfileRequest request) {
		User user = getRequiredUserWithRoles(userId);
		userMapper.updateUserFromProfileRequest(request, user);
		return userMapper.toUserProfileResponse(userRepository.save(user));
	}

	@Transactional
	public void changeCurrentUserPassword(Long userId, ChangePasswordRequest request) {
		User user = getRequiredUserWithRoles(userId);
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new AppException(ErrorCode.INVALID_CURRENT_PASSWORD);
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public PageResponse<UserSummaryResponse> getUsers(int page, int size, String sortBy, String sortDir) {
		Pageable pageable = PageRequest.of(
				page,
				size,
				Sort.by(resolveDirection(sortDir), normalizeSortProperty(sortBy))
		);
		Page<UserSummaryResponse> responsePage = userRepository.findAll(pageable)
				.map(userMapper::toUserSummaryResponse);
		return PageResponse.from(responsePage);
	}

	@Transactional(readOnly = true)
	public UserProfileResponse getUserById(Long id) {
		return userMapper.toUserProfileResponse(getRequiredUserWithRoles(id));
	}

	@Transactional
	public UserProfileResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
		if (request.status() != UserStatus.ACTIVE && request.status() != UserStatus.BANNED) {
			throw new AppException(ErrorCode.INVALID_USER_STATUS);
		}

		User user = getRequiredUserWithRoles(id);
		user.setStatus(request.status());
		return userMapper.toUserProfileResponse(userRepository.save(user));
	}

	@Transactional
	public void resetUserPassword(Long id, ResetPasswordRequest request) {
		User user = getRequiredUserWithRoles(id);
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
	}

	@Transactional
	public User ensureAdminUser(String email, String rawPassword, String fullName) {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		User user = userRepository.findWithRolesByEmail(normalizedEmail).orElseGet(User::new);

		boolean isNewUser = user.getId() == null;
		if (isNewUser) {
			user.setEmail(normalizedEmail);
			user.setPasswordHash(passwordEncoder.encode(rawPassword));
			user.setFullName(fullName.trim());
			user.setStatus(UserStatus.ACTIVE);
			user = userRepository.save(user);
		}

		Role adminRole = roleRepository.findByName(com.affismart.mall.common.enums.RoleName.ADMIN)
				.orElseThrow(() -> new AppException(ErrorCode.ADMIN_ROLE_NOT_FOUND));

		boolean hasAdminRole = user.getUserRoles().stream()
				.anyMatch(userRole -> userRole.getRole().getName() == com.affismart.mall.common.enums.RoleName.ADMIN);
		if (!hasAdminRole) {
			UserRole userRole = userRoleRepository.save(new UserRole(user, adminRole));
			user.getUserRoles().add(userRole);
		}

		if (user.getStatus() != UserStatus.ACTIVE) {
			user.setStatus(UserStatus.ACTIVE);
			user = userRepository.save(user);
		}

		return user;
	}

	private User getRequiredUserWithRoles(Long userId) {
		return userRepository.findWithRolesById(userId)
				.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
	}

	private Sort.Direction resolveDirection(String sortDir) {
		return "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
	}

	private String normalizeSortProperty(String sortBy) {
		if (sortBy == null || sortBy.isBlank()) {
			return "createdAt";
		}
		return switch (sortBy) {
			case "email", "fullName", "status", "createdAt", "updatedAt" -> sortBy;
			default -> "createdAt";
		};
	}
}
