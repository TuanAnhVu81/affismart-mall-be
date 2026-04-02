package com.affismart.mall.modules.user.repository;

import com.affismart.mall.common.enums.RoleName;
import com.affismart.mall.modules.user.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByName(RoleName name);
}
