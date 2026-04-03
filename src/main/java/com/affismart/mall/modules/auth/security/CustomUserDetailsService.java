package com.affismart.mall.modules.auth.security;

import com.affismart.mall.common.enums.UserStatus;
import com.affismart.mall.modules.user.entity.User;
import com.affismart.mall.modules.user.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findWithRolesByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new DisabledException("User account is not active");
		}

		return UserPrincipal.from(user);
	}
}
