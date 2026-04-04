package com.affismart.mall.modules.user.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.common.response.PageResponse;
import com.affismart.mall.modules.auth.security.UserPrincipal;
import com.affismart.mall.modules.user.dto.request.ChangePasswordRequest;
import com.affismart.mall.modules.user.dto.request.ResetPasswordRequest;
import com.affismart.mall.modules.user.dto.request.UpdateProfileRequest;
import com.affismart.mall.modules.user.dto.request.UpdateUserStatusRequest;
import com.affismart.mall.modules.user.dto.response.UserProfileResponse;
import com.affismart.mall.modules.user.dto.response.UserSummaryResponse;
import com.affismart.mall.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.success("Profile retrieved successfully", userService.getCurrentUserProfile(principal.getUserId()));
	}

	@PutMapping("/me")
	public ApiResponse<UserProfileResponse> updateMyProfile(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody UpdateProfileRequest request
	) {
		return ApiResponse.success(
				"Profile updated successfully",
				userService.updateCurrentUserProfile(principal.getUserId(), request)
		);
	}

	@PutMapping("/me/password")
	public ApiResponse<Void> changeMyPassword(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody ChangePasswordRequest request
	) {
		userService.changeCurrentUserPassword(principal.getUserId(), request);
		return ApiResponse.success("Password changed successfully");
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping
	public ApiResponse<PageResponse<UserSummaryResponse>> getUsers(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir
	) {
		return ApiResponse.success("Users retrieved successfully", userService.getUsers(page, size, sortBy, sortDir));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/{id}")
	public ApiResponse<UserProfileResponse> getUserById(@PathVariable Long id) {
		return ApiResponse.success("User retrieved successfully", userService.getUserById(id));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}/status")
	public ApiResponse<UserProfileResponse> updateUserStatus(
			@PathVariable Long id,
			@Valid @RequestBody UpdateUserStatusRequest request
	) {
		return ApiResponse.success("User status updated successfully", userService.updateUserStatus(id, request));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}/reset-password")
	public ApiResponse<Void> resetUserPassword(
			@PathVariable Long id,
			@Valid @RequestBody ResetPasswordRequest request
	) {
		userService.resetUserPassword(id, request);
		return ApiResponse.success("User password reset successfully");
	}
}
