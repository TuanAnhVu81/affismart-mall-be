package com.affismart.mall.config;

import com.affismart.mall.modules.affiliate.interceptor.BlockedIpInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AffiliateWebMvcConfig implements WebMvcConfigurer {

	private final BlockedIpInterceptor blockedIpInterceptor;

	public AffiliateWebMvcConfig(BlockedIpInterceptor blockedIpInterceptor) {
		this.blockedIpInterceptor = blockedIpInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(blockedIpInterceptor)
				.addPathPatterns("/api/v1/affiliate/track-click");
	}
}
