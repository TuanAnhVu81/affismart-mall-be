package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.modules.affiliate.entity.PayoutRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT pr FROM PayoutRequest pr WHERE pr.id = :id")
	Optional<PayoutRequest> findByIdForUpdate(@Param("id") Long id);
}
