package com.affismart.mall.modules.auth.mapper;

import com.affismart.mall.modules.auth.dto.request.RegisterRequest;
import com.affismart.mall.modules.auth.dto.response.AuthUserResponse;
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
public interface AuthMapper {

    /**
     * Maps RegisterRequest to User entity.
     * passwordHash and status must be set manually in AuthService
     * after encoding the raw password and assigning the default status.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "bankInfo", ignore = true)
    @Mapping(target = "defaultShippingAddress", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUser(RegisterRequest request);

    /**
     * Trims fullName and normalizes the optional phone field
     * after the primary mapping from RegisterRequest to User is complete.
     */
    @AfterMapping
    default void normalizeUserFields(@MappingTarget User user) {
        if (user.getFullName() != null) {
            user.setFullName(user.getFullName().trim());
        }
        // Normalize phone: set to null if blank, otherwise trim
        if (!StringUtils.hasText(user.getPhone())) {
            user.setPhone(null);
        } else {
            user.setPhone(user.getPhone().trim());
        }
    }

    /**
     * Maps a User entity to AuthUserResponse DTO.
     * Converts UserStatus enum to its name string,
     * and extracts role names from the UserRole join entities.
     */
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "roles", source = "userRoles")
    AuthUserResponse toAuthUserResponse(User user);

    /**
     * Extracts role name strings from the set of UserRole join entities.
     */
    default List<String> mapUserRolesToRoleNames(Set<UserRole> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }
        return userRoles.stream()
                .map(userRole -> userRole.getRole().getName().name())
                .toList();
    }
}
