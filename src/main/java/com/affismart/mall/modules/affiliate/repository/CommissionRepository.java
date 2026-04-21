package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.modules.affiliate.entity.Commission;
import com.affismart.mall.common.enums.CommissionStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

public interface CommissionRepository extends JpaRepository<Commission, Long>, JpaSpecificationExecutor<Commission> {

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

	@Override
	@EntityGraph(attributePaths = {"order", "payoutRequest"})
	Page<Commission> findAll(Specification<Commission> specification, Pageable pageable);

	@Query("""
			SELECT COUNT(c)
			FROM Commission c
			WHERE c.affiliateAccount.id = :affiliateAccountId
			  AND c.status <> :status
			""")
	long countByAffiliateAccountIdAndStatusNot(
			@Param("affiliateAccountId") Long affiliateAccountId,
			@Param("status") CommissionStatus status
	);

	@Query("""
			SELECT COALESCE(SUM(c.amount), 0)
			FROM Commission c
			WHERE c.affiliateAccount.id = :affiliateAccountId
			  AND c.status IN :statuses
			""")
	BigDecimal sumAmountByAffiliateAccountIdAndStatusIn(
			@Param("affiliateAccountId") Long affiliateAccountId,
			@Param("statuses") Collection<CommissionStatus> statuses
	);
}
