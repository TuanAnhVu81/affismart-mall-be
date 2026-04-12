package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.common.enums.AffiliateAccountStatus;
import com.affismart.mall.modules.affiliate.entity.AffiliateAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AffiliateAccountRepository extends JpaRepository<AffiliateAccount, Long> {

	boolean existsByUser_Id(Long userId);

	boolean existsByRefCode(String refCode);

	Optional<AffiliateAccount> findByUser_Id(Long userId);

	Optional<AffiliateAccount> findByUser_IdAndStatus(Long userId, AffiliateAccountStatus status);

	@EntityGraph(attributePaths = {"user"})
	Optional<AffiliateAccount> findWithUserById(Long id);
}
