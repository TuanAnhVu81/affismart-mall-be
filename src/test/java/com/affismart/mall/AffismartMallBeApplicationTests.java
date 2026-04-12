package com.affismart.mall;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.affismart.mall.modules.user.repository.UserRepository;
import com.affismart.mall.modules.user.repository.RoleRepository;
import com.affismart.mall.modules.user.repository.UserRoleRepository;
import com.affismart.mall.modules.product.repository.CategoryRepository;
import com.affismart.mall.modules.product.repository.ProductRepository;
import com.affismart.mall.modules.order.repository.OrderRepository;
import com.affismart.mall.modules.order.repository.OrderItemRepository;
import com.affismart.mall.modules.order.repository.AffiliateAccountLookupRepository;
import com.affismart.mall.modules.order.repository.CommissionMaintenanceRepository;
import com.affismart.mall.modules.order.service.OrderPaymentGateway;
import com.affismart.mall.modules.order.mapper.OrderMapper;
import com.affismart.mall.modules.product.mapper.ProductMapper;
import com.affismart.mall.modules.affiliate.mapper.AffiliateMapper;
import com.affismart.mall.modules.affiliate.repository.AffiliateAccountRepository;
import com.affismart.mall.modules.affiliate.repository.ReferralLinkRepository;
import com.affismart.mall.modules.affiliate.repository.BlockedClickLogRepository;
import com.affismart.mall.modules.affiliate.repository.CommissionRepository;
import com.affismart.mall.modules.affiliate.repository.PayoutRequestRepository;
import com.affismart.mall.config.RestAuthenticationEntryPoint;
import com.affismart.mall.config.RestAccessDeniedHandler;
import com.affismart.mall.modules.auth.security.JwtAuthenticationFilter;
import org.springframework.security.core.userdetails.UserDetailsService;

@SpringBootTest
class AffismartMallBeApplicationTests {

	@MockitoBean
	private StringRedisTemplate stringRedisTemplate;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private RoleRepository roleRepository;

	@MockitoBean
	private UserRoleRepository userRoleRepository;

	@MockitoBean
	private CategoryRepository categoryRepository;

	@MockitoBean
	private ProductRepository productRepository;

	@MockitoBean
	private OrderRepository orderRepository;

	@MockitoBean
	private OrderItemRepository orderItemRepository;

	@MockitoBean
	private AffiliateAccountLookupRepository affiliateAccountLookupRepository;

	@MockitoBean
	private CommissionMaintenanceRepository commissionMaintenanceRepository;

	@MockitoBean
	private OrderPaymentGateway orderPaymentGateway;

	@MockitoBean
	private OrderMapper orderMapper;

	@MockitoBean
	private ProductMapper productMapper;

	@MockitoBean
	private AffiliateMapper affiliateMapper;

	@MockitoBean
	private AffiliateAccountRepository affiliateAccountRepository;

	@MockitoBean
	private ReferralLinkRepository referralLinkRepository;

	@MockitoBean
	private BlockedClickLogRepository blockedClickLogRepository;

	@MockitoBean
	private CommissionRepository commissionRepository;

	@MockitoBean
	private PayoutRequestRepository payoutRequestRepository;

	@MockitoBean
	private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

	@MockitoBean
	private RestAccessDeniedHandler restAccessDeniedHandler;

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockitoBean(name = "customUserDetailsService")
	private UserDetailsService userDetailsService;

	@Test
	void contextLoads() {
	}

}
