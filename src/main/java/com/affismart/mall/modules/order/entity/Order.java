package com.affismart.mall.modules.order.entity;

import com.affismart.mall.common.entity.BaseEntity;
import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.modules.user.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "orders")
public class Order extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "affiliate_account_id")
	private Long affiliateAccountId;

	@Column(name = "referral_link_id")
	private Long referralLinkId;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountAmount = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status = OrderStatus.PENDING;

	@Column(name = "stripe_session_id", unique = true, length = 255)
	private String stripeSessionId;

	@Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
	private String shippingAddress;

	@ToString.Exclude
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<OrderItem> orderItems = new HashSet<>();

	public void addOrderItem(OrderItem orderItem) {
		orderItem.setOrder(this);
		orderItems.add(orderItem);
	}

	@PrePersist
	@PreUpdate
	private void syncTotalAmount() {
		BigDecimal calculatedTotal = orderItems.stream()
				.map(item -> item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity().longValue())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		this.totalAmount = calculatedTotal.subtract(this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO);

		if (this.totalAmount.compareTo(BigDecimal.ZERO) < 0) {
			this.totalAmount = BigDecimal.ZERO;
		}
	}
}
