package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.modules.affiliate.entity.Commission;
import com.affismart.mall.common.enums.CommissionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommissionRepository extends JpaRepository<Commission, Long> {

	boolean existsByOrder_Id(Long orderId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT c
			FROM Commission c
			WHERE c.affiliateAccount.id = :affiliateAccountId
			  AND c.status = :status
			  AND c.payoutRequest IS NULL
			""")
	List<Commission> findEligibleCommissionsForPayoutForUpdate(
			@Param("affiliateAccountId") Long affiliateAccountId,
			@Param("status") CommissionStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM Commission c WHERE c.payoutRequest.id = :payoutRequestId")
	List<Commission> findByPayoutRequestIdForUpdate(@Param("payoutRequestId") Long payoutRequestId);
}
