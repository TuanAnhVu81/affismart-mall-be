package com.affismart.mall.modules.user.service;

import com.affismart.mall.modules.user.config.AdminBootstrapProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminBootstrapService Unit Tests")
class AdminBootstrapServiceTest {

	@Mock
	private AdminBootstrapProperties properties;

	@Mock
	private UserService userService;

	@Mock
	private ApplicationArguments applicationArguments;

	@InjectMocks
	private AdminBootstrapService adminBootstrapService;

	@Test
	@DisplayName("run: Edge Case - bootstrap disabled does nothing")
	void run_BootstrapDisabled_DoesNothing() {
		// Given
		given(properties.isEnabled()).willReturn(false);

		// When
		adminBootstrapService.run(applicationArguments);

		// Then
		verifyNoInteractions(userService);
	}

	@Test
	@DisplayName("run: Exception Case - enabled bootstrap with missing properties throws IllegalStateException")
	void run_EnabledWithMissingProperties_ThrowsIllegalStateException() {
		// Given
		given(properties.isEnabled()).willReturn(true);
		given(properties.getEmail()).willReturn(" ");

		// When / Then
		assertThatThrownBy(() -> adminBootstrapService.run(applicationArguments))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Admin bootstrap is enabled");

		verifyNoInteractions(userService);
	}

	@Test
	@DisplayName("run: Happy Path - enabled bootstrap delegates to ensureAdminUser")
	void run_EnabledWithValidProperties_CallsEnsureAdminUser() {
		// Given
		given(properties.isEnabled()).willReturn(true);
		given(properties.getEmail()).willReturn("admin@example.com");
		given(properties.getPassword()).willReturn("admin-password");
		given(properties.getFullName()).willReturn("AffiSmart Admin");

		// When
		adminBootstrapService.run(applicationArguments);

		// Then
		verify(userService).ensureAdminUser("admin@example.com", "admin-password", "AffiSmart Admin");
	}
}
