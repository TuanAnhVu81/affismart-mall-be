package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.modules.affiliate.entity.BlockedClickLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BlockedClickLogRepository extends JpaRepository<BlockedClickLog, Long> {

	@Query(
			value = """
					SELECT latest.id,
					       latest.ip_address,
					       latest.reason,
					       latest.expires_at,
					       latest.created_at
					FROM (
					    SELECT DISTINCT ON (b.ip_address)
					           b.id,
					           b.ip_address,
					           b.reason,
					           b.expires_at,
					           b.created_at
					    FROM blocked_click_logs b
					    WHERE b.expires_at IS NULL OR b.expires_at > CURRENT_TIMESTAMP
					    ORDER BY b.ip_address, b.created_at DESC, b.id DESC
					) latest
					ORDER BY latest.created_at DESC, latest.id DESC
					""",
			nativeQuery = true
	)
	List<BlockedClickLog> findActiveBlockedIps();

	long deleteByIpAddress(String ipAddress);
}
