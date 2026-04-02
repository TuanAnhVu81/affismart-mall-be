package com.affismart.mall.modules.user.entity;

import com.affismart.mall.common.entity.BaseEntity;
import com.affismart.mall.common.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(name = "phone", length = 20)
	private String phone;

	@Column(name = "default_shipping_address", columnDefinition = "TEXT")
	private String defaultShippingAddress;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "bank_info", columnDefinition = "TEXT")
	private String bankInfo;

	@ToString.Exclude
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	private Set<UserRole> userRoles = new HashSet<>();
}
