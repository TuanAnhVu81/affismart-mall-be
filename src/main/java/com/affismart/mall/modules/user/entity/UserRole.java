package com.affismart.mall.modules.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_roles")
public class UserRole {

	@EmbeddedId
	private UserRoleId id = new UserRoleId();

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@MapsId("roleId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	@Column(name = "assigned_at", nullable = false, updatable = false)
	private LocalDateTime assignedAt;

	public UserRole(User user, Role role) {
		this.user = user;
		this.role = role;
		syncId();
	}

	@PrePersist
	void prePersist() {
		syncId();
		if (assignedAt == null) {
			assignedAt = LocalDateTime.now();
		}
	}

	private void syncId() {
		if (id == null) {
			id = new UserRoleId();
		}
		if (user != null) {
			id.setUserId(user.getId());
		}
		if (role != null) {
			id.setRoleId(role.getId());
		}
	}
}
