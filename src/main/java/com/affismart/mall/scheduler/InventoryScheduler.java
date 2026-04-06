package com.affismart.mall.scheduler;

import com.affismart.mall.modules.product.dto.response.ProductResponse;
import com.affismart.mall.modules.product.service.ProductService;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryScheduler {

	private static final Logger log = LoggerFactory.getLogger(InventoryScheduler.class);

	private final ProductService productService;
	private final int lowStockThreshold;

	public InventoryScheduler(
			ProductService productService,
			@Value("${app.inventory.low-stock-threshold:10}") int lowStockThreshold
	) {
		this.productService = productService;
		this.lowStockThreshold = Math.max(lowStockThreshold, 1);
	}

	@Scheduled(
			cron = "${app.inventory.low-stock-cron:0 0 0 * * ?}",
			zone = "${app.timezone:Asia/Bangkok}"
	)
	public void monitorLowStockProducts() {
		List<ProductResponse> lowStockProducts = productService.getLowStockProducts(lowStockThreshold);
		if (lowStockProducts.isEmpty()) {
			log.info("Inventory monitor finished: no products below threshold {}", lowStockThreshold);
			return;
		}

		String sampleSkus = lowStockProducts.stream()
				.limit(10)
				.map(ProductResponse::sku)
				.collect(Collectors.joining(", "));
		log.warn(
				"Inventory alert: {} products below threshold {}. Sample SKUs: {}",
				lowStockProducts.size(),
				lowStockThreshold,
				sampleSkus
		);
	}
}
