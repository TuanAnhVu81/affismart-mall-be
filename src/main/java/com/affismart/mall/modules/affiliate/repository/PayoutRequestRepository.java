package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.modules.affiliate.entity.PayoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, Long> {
}
