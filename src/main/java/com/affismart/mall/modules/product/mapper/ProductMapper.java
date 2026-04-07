package com.affismart.mall.modules.product.mapper;

import com.affismart.mall.modules.product.dto.request.UpsertProductRequest;
import com.affismart.mall.modules.product.dto.response.ProductResponse;
import com.affismart.mall.modules.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

	default Product toProductEntity(UpsertProductRequest request) {
		if (request == null) {
			return null;
		}
		Product product = new Product();
		updateProductFromRequest(request, product);
		return product;
	}

	default void updateProductFromRequest(UpsertProductRequest request, Product product) {
		if (request == null || product == null) {
			return;
		}
		product.setName(request.name());
		product.setSku(request.sku());
		product.setSlug(request.slug());
		product.setDescription(request.description());
		product.setPrice(request.price());
		product.setStockQuantity(request.stockQuantity());
		product.setImageUrl(request.imageUrl());
	}

	@Mapping(target = "categoryId", source = "category.id")
	@Mapping(target = "categoryName", source = "category.name")
	ProductResponse toProductResponse(Product product);
}
