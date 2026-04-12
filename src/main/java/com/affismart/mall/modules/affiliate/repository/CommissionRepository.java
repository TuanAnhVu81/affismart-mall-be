package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.modules.affiliate.entity.Commission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRepository extends JpaRepository<Commission, Long> {
}
