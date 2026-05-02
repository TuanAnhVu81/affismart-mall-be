package com.affismart.mall.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "Paginated response payload")
public record PageResponse<T>(
		@Schema(description = "Current page content")
		List<T> content,

		@Schema(description = "Zero-based page index", example = "0")
		int page,

		@Schema(description = "Requested page size", example = "10")
		int size,

		@Schema(description = "Total number of matching elements", example = "42")
		long totalElements,

		@Schema(description = "Total number of pages", example = "5")
		int totalPages,

		@Schema(description = "Whether this is the last page", example = "false")
		boolean last
) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isLast()
		);
	}
}
