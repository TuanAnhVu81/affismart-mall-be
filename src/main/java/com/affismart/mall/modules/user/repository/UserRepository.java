package com.affismart.mall.modules.user.repository;

import com.affismart.mall.modules.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	@EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
	Optional<User> findWithRolesByEmail(String email);

	@EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
	Optional<User> findWithRolesById(Long id);

	Page<User> findAll(Pageable pageable);

	boolean existsByEmail(String email);
}
