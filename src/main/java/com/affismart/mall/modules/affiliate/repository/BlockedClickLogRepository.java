package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.modules.affiliate.entity.BlockedClickLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedClickLogRepository extends JpaRepository<BlockedClickLog, Long> {
}
