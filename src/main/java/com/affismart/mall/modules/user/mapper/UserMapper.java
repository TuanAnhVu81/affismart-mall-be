package com.affismart.mall.modules.user.mapper;

import com.affismart.mall.modules.user.dto.request.UpdateProfileRequest;
import com.affismart.mall.modules.user.dto.response.UserProfileResponse;
import com.affismart.mall.modules.user.dto.response.UserSummaryResponse;
import com.affismart.mall.modules.user.entity.User;
import com.affismart.mall.modules.user.entity.UserRole;
import java.util.List;
import java.util.Set;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.util.StringUtils;

@Mapper(
		componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

	@Mapping(target = "status", expression = "java(user.getStatus().name())")
	@Mapping(target = "roles", source = "userRoles")
	UserProfileResponse toUserProfileResponse(User user);

	@Mapping(target = "status", expression = "java(user.getStatus().name())")
	@Mapping(target = "roles", source = "userRoles")
	UserSummaryResponse toUserSummaryResponse(User user);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "email", ignore = true)
	@Mapping(target = "passwordHash", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "bankInfo", ignore = true)
	@Mapping(target = "userRoles", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void updateUserFromProfileRequest(UpdateProfileRequest request, @MappingTarget User user);

	@AfterMapping
	default void normalizeUserFields(@MappingTarget User user) {
		if (user.getFullName() != null) {
			user.setFullName(user.getFullName().trim());
		}
		if (!StringUtils.hasText(user.getPhone())) {
			user.setPhone(null);
		} else {
			user.setPhone(user.getPhone().trim());
		}
		if (!StringUtils.hasText(user.getDefaultShippingAddress())) {
			user.setDefaultShippingAddress(null);
		} else {
			user.setDefaultShippingAddress(user.getDefaultShippingAddress().trim());
		}
	}

	default List<String> mapUserRolesToRoleNames(Set<UserRole> userRoles) {
		if (userRoles == null || userRoles.isEmpty()) {
			return List.of();
		}
		return userRoles.stream()
				.map(userRole -> userRole.getRole().getName().name())
				.toList();
	}
}
