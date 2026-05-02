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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "Endpoints for user profile and administrative user control")
@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@Operation(summary = "Get current user profile", description = "Retrieves information for the currently authenticated user")
	@GetMapping("/me")
	public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.success("Profile retrieved successfully", userService.getCurrentUserProfile(principal.getUserId()));
	}

	@Operation(summary = "Update current user profile", description = "Updates basic information for the currently authenticated user")
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

	@Operation(summary = "Change current user password", description = "Requires providing and verifying the current password")
	@PutMapping("/me/password")
	public ApiResponse<Void> changeMyPassword(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody ChangePasswordRequest request
	) {
		userService.changeCurrentUserPassword(principal.getUserId(), request);
		return ApiResponse.success("Password changed successfully");
	}

	@Operation(summary = "List all users (Admin only)", description = "Retrieves a paginated and sorted list of all users in the system")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping
	public ApiResponse<PageResponse<UserSummaryResponse>> getUsers(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir
	) {
		return ApiResponse.success("Users retrieved successfully", userService.getUsers(page, size, sortBy, sortDir));
	}

	@Operation(summary = "Get user by ID (Admin only)", description = "Retrieves detailed information for a specific user")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/{id}")
	public ApiResponse<UserProfileResponse> getUserById(@PathVariable @Positive Long id) {
		return ApiResponse.success("User retrieved successfully", userService.getUserById(id));
	}

	@Operation(summary = "Update user status (Admin only)", description = "Activates or bans a user account")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}/status")
	public ApiResponse<UserProfileResponse> updateUserStatus(
			@PathVariable @Positive Long id,
			@Valid @RequestBody UpdateUserStatusRequest request
	) {
		return ApiResponse.success("User status updated successfully", userService.updateUserStatus(id, request));
	}

	@Operation(summary = "Reset user password (Admin only)", description = "Allows administrator to force-set a new password for a user")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}/reset-password")
	public ApiResponse<Void> resetUserPassword(
			@PathVariable @Positive Long id,
			@Valid @RequestBody ResetPasswordRequest request
	) {
		userService.resetUserPassword(id, request);
		return ApiResponse.success("User password reset successfully");
	}
}
