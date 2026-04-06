package com.affismart.mall.modules.product.service;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.util.StringUtils;

final class SlugUtils {

	private SlugUtils() {
	}

	static String toSlug(String input) {
		if (!StringUtils.hasText(input)) {
			return "";
		}

		String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return normalized.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-|-$)", "");
	}
}
