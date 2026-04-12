package com.affismart.mall.modules.affiliate.entity;

import com.affismart.mall.common.entity.BaseEntity;
import com.affismart.mall.common.enums.AffiliateAccountStatus;
import com.affismart.mall.modules.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "affiliate_accounts")
public class AffiliateAccount extends BaseEntity {

	private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("10.00");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ToString.Exclude
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "ref_code", nullable = false, unique = true, length = 50)
	private String refCode;

	@Column(name = "promotion_channel", length = 100)
	private String promotionChannel;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private AffiliateAccountStatus status = AffiliateAccountStatus.PENDING;

	@Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
	private BigDecimal commissionRate = DEFAULT_COMMISSION_RATE;

	@Column(name = "balance", nullable = false, precision = 12, scale = 2)
	private BigDecimal balance = BigDecimal.ZERO;

	@ToString.Exclude
	@OneToMany(mappedBy = "affiliateAccount", fetch = FetchType.LAZY)
	private Set<ReferralLink> referralLinks = new HashSet<>();
}
