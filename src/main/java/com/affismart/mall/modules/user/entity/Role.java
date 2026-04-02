package com.affismart.mall.modules.user.entity;

import com.affismart.mall.common.enums.RoleName;
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
@Table(name = "roles")
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "name", nullable = false, unique = true, length = 50)
	private RoleName name;

	@Column(name = "description", length = 255)
	private String description;

	@ToString.Exclude
	@OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
	private Set<UserRole> userRoles = new HashSet<>();
}
