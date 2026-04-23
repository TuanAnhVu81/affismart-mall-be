package com.affismart.mall.modules.ai.service;

import com.affismart.mall.modules.ai.entity.RecommendationEvent;
import com.affismart.mall.modules.ai.repository.RecommendationEventRepository;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationEventWriter {

	private final RecommendationEventRepository recommendationEventRepository;

	public RecommendationEventWriter(RecommendationEventRepository recommendationEventRepository) {
		this.recommendationEventRepository = recommendationEventRepository;
	}

	@Async("recommendationEventExecutor")
	@Transactional
	public CompletableFuture<Void> writeAsync(RecommendationEvent recommendationEvent) {
		recommendationEventRepository.save(recommendationEvent);
		return CompletableFuture.completedFuture(null);
	}
}
