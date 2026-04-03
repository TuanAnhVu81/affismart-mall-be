package com.affismart.mall.modules.auth.security;

import com.affismart.mall.common.enums.UserStatus;
import com.affismart.mall.modules.user.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

	private final Long userId;
	private final String email;
	private final String password;
	private final Collection<? extends GrantedAuthority> authorities;
	private final boolean enabled;
	private final boolean accountNonLocked;

	public UserPrincipal(
			Long userId,
			String email,
			String password,
			Collection<? extends GrantedAuthority> authorities,
			boolean enabled,
			boolean accountNonLocked
	) {
		this.userId = userId;
		this.email = email;
		this.password = password;
		this.authorities = List.copyOf(authorities);
		this.enabled = enabled;
		this.accountNonLocked = accountNonLocked;
	}

	public static UserPrincipal from(User user) {
		List<SimpleGrantedAuthority> authorities = user.getUserRoles().stream()
				.map(userRole -> userRole.getRole().getName().name())
				.map(roleName -> "ROLE_" + roleName)
				.map(SimpleGrantedAuthority::new)
				.toList();

		return new UserPrincipal(
				user.getId(),
				user.getEmail(),
				user.getPasswordHash(),
				authorities,
				user.getStatus() == UserStatus.ACTIVE,
				user.getStatus() != UserStatus.BANNED
		);
	}

	public Long getUserId() {
		return userId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return accountNonLocked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
