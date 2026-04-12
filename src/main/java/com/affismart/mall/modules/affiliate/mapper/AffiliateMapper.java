package com.affismart.mall.modules.affiliate.mapper;

import com.affismart.mall.modules.affiliate.dto.response.AffiliateAccountResponse;
import com.affismart.mall.modules.affiliate.dto.response.ReferralLinkResponse;
import com.affismart.mall.modules.affiliate.entity.AffiliateAccount;
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

	@Mapping(target = "affiliateAccountId", source = "affiliateAccount.id")
	@Mapping(target = "productId", source = "product.id")
	ReferralLinkResponse toReferralLinkResponse(ReferralLink referralLink);
}
