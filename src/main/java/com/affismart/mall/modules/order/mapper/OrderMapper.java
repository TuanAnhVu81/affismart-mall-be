package com.affismart.mall.modules.order.mapper;

import com.affismart.mall.modules.order.dto.response.CreateOrderResponse;
import com.affismart.mall.modules.order.dto.response.OrderDetailResponse;
import com.affismart.mall.modules.order.dto.response.OrderItemDetailResponse;
import com.affismart.mall.modules.order.dto.response.OrderSummaryResponse;
import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.entity.OrderItem;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OrderMapper {

	@Mapping(target = "orderId", source = "id")
	CreateOrderResponse toCreateOrderResponse(Order order);

	@Mapping(target = "status", expression = "java(order.getStatus().name())")
	OrderSummaryResponse toOrderSummaryResponse(Order order);

	@Mapping(target = "status", expression = "java(order.getStatus().name())")
	@Mapping(target = "items", source = "items")
	OrderDetailResponse toOrderDetailResponse(Order order, List<OrderItem> items);

	@Mapping(target = "productId", source = "product.id")
	@Mapping(target = "productName", source = "product.name")
	@Mapping(target = "productSku", source = "product.sku")
	@Mapping(target = "lineTotal", source = "item", qualifiedByName = "calculateLineTotal")
	OrderItemDetailResponse toOrderItemDetailResponse(OrderItem item);

	@Named("calculateLineTotal")
	default BigDecimal calculateLineTotal(OrderItem item) {
		if (item.getPriceAtTime() == null || item.getQuantity() == null) {
			return BigDecimal.ZERO;
		}
		return item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity().longValue()));
	}
}
