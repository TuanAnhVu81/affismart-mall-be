package com.affismart.mall;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

@SpringBootTest
class AffismartMallBeApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class TestBeans {

		@Bean
		StringRedisTemplate stringRedisTemplate() {
			return mock(StringRedisTemplate.class);
		}
	}

}
