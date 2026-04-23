package com.affismart.mall.modules.ai.repository;

import com.affismart.mall.modules.ai.entity.RecommendationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationEventRepository extends JpaRepository<RecommendationEvent, Long> {
}
