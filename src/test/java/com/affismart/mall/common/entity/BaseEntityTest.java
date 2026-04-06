package com.affismart.mall.common.entity;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BaseEntity Unit Tests")
class BaseEntityTest {

	// =========================================================
	// prePersist()
	// =========================================================

	@Test
	@DisplayName("prePersist: Happy Path - sets both createdAt and updatedAt when both are null")
	void prePersist_WhenTimestampsAreNull_SetsBothValues() {
		// Given
		TestEntity entity = new TestEntity();
		assertThat(entity.getCreatedAt()).isNull();
		assertThat(entity.getUpdatedAt()).isNull();

		// When
		entity.callPrePersist();

		// Then - both timestamps must be populated after first persist
		assertThat(entity.getCreatedAt()).isNotNull();
		assertThat(entity.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("prePersist: Edge Case - keeps existing createdAt and only fills missing updatedAt")
	void prePersist_WhenCreatedAtExists_KeepsCreatedAtAndFillsUpdatedAt() {
		// Given
		TestEntity entity = new TestEntity();
		LocalDateTime fixedCreatedAt = LocalDateTime.of(2026, 4, 1, 8, 0, 0);
		entity.setCreatedAt(fixedCreatedAt);

		// When
		entity.callPrePersist();

		// Then - createdAt must be preserved, updatedAt must be filled
		assertThat(entity.getCreatedAt()).isEqualTo(fixedCreatedAt);
		assertThat(entity.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("prePersist: Edge Case - does not overwrite either timestamp when both already exist")
	void prePersist_WhenBothTimestampsExist_KeepsBothUnchanged() {
		// Given
		TestEntity entity = new TestEntity();
		LocalDateTime fixedCreatedAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
		LocalDateTime fixedUpdatedAt = LocalDateTime.of(2026, 2, 1, 0, 0, 0);
		entity.setCreatedAt(fixedCreatedAt);
		entity.setUpdatedAt(fixedUpdatedAt);

		// When
		entity.callPrePersist();

		// Then - both timestamps must remain untouched
		assertThat(entity.getCreatedAt()).isEqualTo(fixedCreatedAt);
		assertThat(entity.getUpdatedAt()).isEqualTo(fixedUpdatedAt);
	}

	// =========================================================
	// preUpdate()
	// =========================================================

	@Test
	@DisplayName("preUpdate: Happy Path - always refreshes updatedAt to current time")
	void preUpdate_AlwaysRefreshesUpdatedAt() {
		// Given
		TestEntity entity = new TestEntity();
		LocalDateTime oldUpdatedAt = LocalDateTime.of(2026, 4, 1, 8, 0, 0);
		entity.setUpdatedAt(oldUpdatedAt);

		// When
		entity.callPreUpdate();

		// Then - updatedAt must be a newer timestamp than the old one
		assertThat(entity.getUpdatedAt()).isAfter(oldUpdatedAt);
	}

	@Test
	@DisplayName("preUpdate: Edge Case - sets updatedAt even when it was previously null")
	void preUpdate_WhenUpdatedAtIsNull_SetsValue() {
		// Given
		TestEntity entity = new TestEntity();
		assertThat(entity.getUpdatedAt()).isNull();

		// When
		entity.callPreUpdate();

		// Then - updatedAt must be populated even from null state
		assertThat(entity.getUpdatedAt()).isNotNull();
	}

	// =========================================================
	// Private Helper: Concrete subclass to expose protected methods
	// =========================================================

	private static class TestEntity extends BaseEntity {

		void callPrePersist() {
			prePersist();
		}

		void callPreUpdate() {
			preUpdate();
		}
	}
}
