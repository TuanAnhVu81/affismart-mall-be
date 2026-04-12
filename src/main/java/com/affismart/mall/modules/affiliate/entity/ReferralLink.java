package com.affismart.mall.modules.affiliate.entity;

import com.affismart.mall.common.entity.BaseEntity;
import com.affismart.mall.modules.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "referral_links")
public class ReferralLink extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "affiliate_account_id", nullable = false)
	private AffiliateAccount affiliateAccount;

	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "ref_code", nullable = false, unique = true, length = 20)
	private String refCode;

	@Column(name = "total_clicks", nullable = false)
	private Integer totalClicks = 0;

	@Column(name = "total_conversions", nullable = false)
	private Integer totalConversions = 0;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;
}
