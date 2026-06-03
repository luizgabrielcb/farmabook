package br.com.luizgabriel.farmaorder.commons;

import br.com.luizgabriel.farmaorder.notification.dto.NotificationGetResponse;
import br.com.luizgabriel.farmaorder.stock.Category;
import br.com.luizgabriel.farmaorder.stock.order.*;
import br.com.luizgabriel.farmaorder.stock.order.dto.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class OrderUtils {

    public static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000050");
    public static final UUID ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000051");
    public static final UUID OTHER_ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000052");

    public OrderItem newItem(UUID id, OrderItemStatus status) {
        return OrderItem.builder()
                .id(id)
                .product("Dipirona 500mg")
                .category(Category.GENERICO)
                .quantity(1)
                .status(status)
                .build();
    }

    public OrderItem newPendingItem() {
        return newItem(ITEM_ID, OrderItemStatus.PENDING);
    }

    public OrderItem newOrderedItem() {
        return newItem(ITEM_ID, OrderItemStatus.ORDERED);
    }

    public OrderItem newReceivedItem() {
        return newItem(ITEM_ID, OrderItemStatus.RECEIVED);
    }

    public OrderItem newDeliveredItem() {
        return newItem(ITEM_ID, OrderItemStatus.DELIVERED);
    }

    private Order buildOrder(OrderStatus status, OrderItem... items) {
        return Order.builder()
                .id(ORDER_ID)
                .customerId(CustomerUtils.CUSTOMER_ID)
                .customerName("Test Customer")
                .status(status)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .items(new ArrayList<>(Arrays.asList(items)))
                .build();
    }

    public Order newOrder() {
        return buildOrder(OrderStatus.PENDING, newPendingItem());
    }

    public Order newOrderedOrder() {
        return buildOrder(OrderStatus.ORDERED, newOrderedItem());
    }

    public Order newReceivedOrder() {
        return buildOrder(OrderStatus.RECEIVED, newReceivedItem());
    }

    public Order newDeliveredOrder() {
        return buildOrder(OrderStatus.DELIVERED, newDeliveredItem());
    }

    public Order newOrderWithDeliveredItem() {
        return buildOrder(OrderStatus.PENDING,
                newPendingItem(),
                newItem(OTHER_ITEM_ID, OrderItemStatus.DELIVERED));
    }

    public OrderPostRequest newOrderPostRequest() {
        return new OrderPostRequest(CustomerUtils.CUSTOMER_ID, List.of(newOrderItemPostRequest()));
    }

    public OrderItemPostRequest newOrderItemPostRequest() {
        return new OrderItemPostRequest("Dipirona 500mg", Category.GENERICO, 1);
    }

    public OrderItemPutRequest newOrderItemPutRequest() {
        return new OrderItemPutRequest("Paracetamol 750mg", Category.ETICO, 2);
    }

    public OrderPutRequest newOrderPutRequest() {
        return new OrderPutRequest(CustomerUtils.OTHER_CUSTOMER_ID);
    }

    public OrderPostResponse newOrderPostResponse(Order order) {
        return new OrderPostResponse(order.getId(), order.getCustomerId(), order.getCustomerName(),
                order.getStatus(), order.getCreatedById(), order.getCreatedByName(), Instant.now(), List.of());
    }

    public OrderPutResponse newOrderPutResponse(Order order) {
        return new OrderPutResponse(order.getId(), order.getCustomerId(), order.getCustomerName(),
                order.getStatus(), Instant.now());
    }

    public OrderGetResponse newOrderGetResponse(Order order) {
        return new OrderGetResponse(order.getId(), order.getCustomerId(), order.getCustomerName(),
                order.getStatus(), order.getNotifiedAt(), order.getCreatedById(), order.getCreatedByName(),
                Instant.now(), Instant.now(), List.of());
    }

    public OrderItemGetResponse newOrderItemGetResponse(OrderItem item) {
        return new OrderItemGetResponse(item.getId(), item.getProduct(), item.getCategory(), item.getQuantity(),
                item.getStatus(), item.getOrderedById(), item.getOrderedByName(), item.getOrderedAt(),
                item.getReceivedById(), item.getReceivedByName(), item.getReceivedAt(),
                item.getDeliveredById(), item.getDeliveredByName(), item.getDeliveredAt(),
                Instant.now(), Instant.now());
    }

    public NotificationGetResponse newNotificationGetResponse() {
        return new NotificationGetResponse(UUID.randomUUID(), ORDER_ID, CustomerUtils.CUSTOMER_ID,
                "5511999999999", "Test Customer", "Boa tarde, Test Customer!",
                "https://wa.me/5511999999999", Instant.now());
    }
}
