package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.common.enums.AffiliateAccountStatus;
import com.affismart.mall.modules.affiliate.entity.AffiliateAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AffiliateAccountRepository extends JpaRepository<AffiliateAccount, Long> {

	boolean existsByUser_Id(Long userId);

	boolean existsByRefCode(String refCode);

	Optional<AffiliateAccount> findByUser_Id(Long userId);

	Optional<AffiliateAccount> findByUser_IdAndStatus(Long userId, AffiliateAccountStatus status);

	Optional<AffiliateAccount> findByIdAndStatus(Long id, AffiliateAccountStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT aa FROM AffiliateAccount aa WHERE aa.id = :id")
	Optional<AffiliateAccount> findByIdForUpdate(@Param("id") Long id);

	@EntityGraph(attributePaths = {"user"})
	Optional<AffiliateAccount> findWithUserById(Long id);
}
