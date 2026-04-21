package com.affismart.mall.modules.affiliate.mapper;

import com.affismart.mall.modules.affiliate.dto.response.AdminAffiliateAccountResponse;
import com.affismart.mall.modules.affiliate.dto.response.AdminPayoutRequestResponse;
import com.affismart.mall.modules.affiliate.dto.response.AffiliateAccountResponse;
import com.affismart.mall.modules.affiliate.dto.response.CommissionResponse;
import com.affismart.mall.modules.affiliate.dto.response.PayoutRequestResponse;
import com.affismart.mall.modules.affiliate.dto.response.ReferralLinkResponse;
import com.affismart.mall.modules.affiliate.entity.AffiliateAccount;
import com.affismart.mall.modules.affiliate.entity.Commission;
import com.affismart.mall.modules.affiliate.entity.PayoutRequest;
import com.affismart.mall.modules.affiliate.entity.ReferralLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AffiliateMapper {

	@Mapping(target = "userId", source = "user.id")
	@Mapping(target = "status", expression = "java(account.getStatus().name())")
	AffiliateAccountResponse toAffiliateAccountResponse(AffiliateAccount account);

	@Mapping(target = "userId", source = "user.id")
	@Mapping(target = "fullName", source = "user.fullName")
	@Mapping(target = "email", source = "user.email")
	@Mapping(target = "bankInfo", source = "user.bankInfo")
	@Mapping(target = "status", expression = "java(account.getStatus().name())")
	AdminAffiliateAccountResponse toAdminAffiliateAccountResponse(AffiliateAccount account);

	@Mapping(target = "affiliateAccountId", source = "affiliateAccount.id")
	@Mapping(target = "productId", source = "product.id")
	@Mapping(target = "productName", source = "product.name")
	ReferralLinkResponse toReferralLinkResponse(ReferralLink referralLink);

	@Mapping(target = "affiliateAccountId", source = "affiliateAccount.id")
	@Mapping(target = "orderId", source = "order.id")
	@Mapping(target = "orderTotalAmount", source = "order.totalAmount")
	@Mapping(target = "orderStatus", expression = "java(commission.getOrder().getStatus().name())")
	@Mapping(target = "status", expression = "java(commission.getStatus().name())")
	@Mapping(target = "payoutRequestId", source = "payoutRequest.id")
	CommissionResponse toCommissionResponse(Commission commission);

	@Mapping(target = "affiliateAccountId", source = "affiliateAccount.id")
	@Mapping(target = "status", expression = "java(payoutRequest.getStatus().name())")
	PayoutRequestResponse toPayoutRequestResponse(PayoutRequest payoutRequest);

	@Mapping(target = "affiliateAccountId", source = "affiliateAccount.id")
	@Mapping(target = "affiliateUserId", source = "affiliateAccount.user.id")
	@Mapping(target = "affiliateFullName", source = "affiliateAccount.user.fullName")
	@Mapping(target = "affiliateEmail", source = "affiliateAccount.user.email")
	@Mapping(target = "affiliateRefCode", source = "affiliateAccount.refCode")
	@Mapping(target = "bankInfo", source = "affiliateAccount.user.bankInfo")
	@Mapping(target = "status", expression = "java(payoutRequest.getStatus().name())")
	AdminPayoutRequestResponse toAdminPayoutRequestResponse(PayoutRequest payoutRequest);
}
