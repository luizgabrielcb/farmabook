package br.com.luizgabriel.farmabook.order;

import br.com.luizgabriel.farmabook.auth.User;
import br.com.luizgabriel.farmabook.customer.Customer;
import br.com.luizgabriel.farmabook.order.dto.*;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "paymentStatus", expression = "java(request.paymentStatus() != null ? request.paymentStatus() : br.com.luizgabriel.farmabook.order.OrderPaymentStatus.TO_PAY)")
    @Mapping(target = "notifiedAt", ignore = true)
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.name")
    @Mapping(target = "observations", source = "request.observations")
    @Mapping(target = "totalPrice", source = "request.totalPrice")
    @Mapping(target = "items", ignore = true)
    Order toOrder(OrderPostRequest request, Customer customer, User createdBy);

    OrderPostResponse toOrderPostResponse(Order order);

    OrderPutResponse toOrderPutResponse(Order order);

    @Mapping(target = "totalPrice", expression = "java(computeTotalPrice(order))")
    OrderGetResponse toOrderGetResponse(Order order);

    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "totalPrice", expression = "java(computeTotalPrice(order, orderItems))")
    OrderGetResponse toOrderGetResponse(Order order, List<OrderItem> orderItems);

    default BigDecimal computeTotalPrice(Order order) {
        return computeTotalPrice(order, order.getItems());
    }

    default BigDecimal computeTotalPrice(Order order, List<OrderItem> items) {
        if (items != null) {
            var total = items.stream()
                    .filter(i -> i.getPrice() != null)
                    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(BigDecimal.ZERO) > 0) return total;
        }
        return order.getTotalPrice();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "paymentStatus", constant = "TO_PAY")
    @Mapping(target = "distributorId", ignore = true)
    @Mapping(target = "distributorName", ignore = true)
    @Mapping(target = "orderedById", ignore = true)
    @Mapping(target = "orderedByName", ignore = true)
    @Mapping(target = "orderedAt", ignore = true)
    @Mapping(target = "receivedById", ignore = true)
    @Mapping(target = "receivedByName", ignore = true)
    @Mapping(target = "receivedAt", ignore = true)
    @Mapping(target = "deliveredById", ignore = true)
    @Mapping(target = "deliveredByName", ignore = true)
    @Mapping(target = "deliveredAt", ignore = true)
    @Mapping(target = "paymentChangedById", ignore = true)
    @Mapping(target = "paymentChangedByName", ignore = true)
    @Mapping(target = "paymentChangedAt", ignore = true)
    OrderItem toOrderItem(OrderItemPostRequest request);

    List<OrderItem> toOrderItems(List<OrderItemPostRequest> requests);

    OrderItemGetResponse toOrderItemGetResponse(OrderItem item);

    List<OrderItemGetResponse> toOrderItemGetResponses(List<OrderItem> items);
}
