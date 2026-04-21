package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.modules.affiliate.entity.PayoutRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.affismart.mall.common.enums.PayoutRequestStatus;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT pr FROM PayoutRequest pr WHERE pr.id = :id")
	Optional<PayoutRequest> findByIdForUpdate(@Param("id") Long id);

	@EntityGraph(attributePaths = {"affiliateAccount"})
	@Query("""
			SELECT pr
			FROM PayoutRequest pr
			WHERE pr.affiliateAccount.id = :affiliateAccountId
			  AND (:status IS NULL OR pr.status = :status)
			""")
	Page<PayoutRequest> findByAffiliateAccountId(
			@Param("affiliateAccountId") Long affiliateAccountId,
			@Param("status") PayoutRequestStatus status,
			Pageable pageable
	);

	@EntityGraph(attributePaths = {"affiliateAccount", "affiliateAccount.user"})
	@Query("""
			SELECT pr
			FROM PayoutRequest pr
			WHERE (:status IS NULL OR pr.status = :status)
			""")
	Page<PayoutRequest> findAllForAdmin(
			@Param("status") PayoutRequestStatus status,
			Pageable pageable
	);
}
