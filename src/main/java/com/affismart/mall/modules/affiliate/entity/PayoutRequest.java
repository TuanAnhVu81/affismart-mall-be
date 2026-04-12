package com.affismart.mall.modules.affiliate.entity;

import com.affismart.mall.common.entity.BaseEntity;
import com.affismart.mall.common.enums.PayoutRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@Table(name = "payout_requests")
public class PayoutRequest extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "affiliate_account_id", nullable = false)
	private AffiliateAccount affiliateAccount;

	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PayoutRequestStatus status = PayoutRequestStatus.PENDING;

	@Column(name = "admin_note", columnDefinition = "TEXT")
	private String adminNote;

	@Column(name = "resolved_at")
	private LocalDateTime resolvedAt;

	@ToString.Exclude
	@OneToMany(mappedBy = "payoutRequest", fetch = FetchType.LAZY)
	private Set<Commission> commissions = new HashSet<>();
}
