package com.affismart.mall.modules.user.repository;

import com.affismart.mall.modules.user.entity.UserRole;
import com.affismart.mall.modules.user.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}
