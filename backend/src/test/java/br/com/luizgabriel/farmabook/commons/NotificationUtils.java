package br.com.luizgabriel.farmabook.commons;

import br.com.luizgabriel.farmabook.customer.Customer;
import br.com.luizgabriel.farmabook.notification.Notification;
import br.com.luizgabriel.farmabook.notification.dto.NotificationGetResponse;
import br.com.luizgabriel.farmabook.order.Order;

import java.time.Instant;
import java.util.UUID;

public class NotificationUtils {

    public static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    public static final UUID NOTIFICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");

    public Order newOrder() {
        return Order.builder()
                .id(ORDER_ID)
                .customerId(CustomerUtils.CUSTOMER_ID)
                .customerName("Test Customer")
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .build();
    }

    public Customer newCustomer() {
        return Customer.builder()
                .id(CustomerUtils.CUSTOMER_ID)
                .name("Test Customer")
                .phoneNumber("+5511999999999")
                .build();
    }

    // Phone without country code — exercises the !startsWith("55") branch in sanitizePhone
    public Customer newCustomerWithLocalPhone() {
        return Customer.builder()
                .id(CustomerUtils.CUSTOMER_ID)
                .name("Test Customer")
                .phoneNumber("(11) 99999-9999")
                .build();
    }

    // Single-word name — exercises the parts.length == 1 branch in shortenName
    public Customer newCustomerWithSingleName() {
        return Customer.builder()
                .id(CustomerUtils.CUSTOMER_ID)
                .name("Joao")
                .phoneNumber("+5511999999999")
                .build();
    }

    public Notification newNotification(Order order, Customer customer) {
        return Notification.builder()
                .id(NOTIFICATION_ID)
                .order(order)
                .customer(customer)
                .customerPhone("5511999999999")
                .customerName(customer.getName())
                .message("Boa tarde, Test Customer! Tudo bem? A sua encomenda acabou de chegar aqui na farmácia.")
                .link("https://wa.me/5511999999999?text=...")
                .sentAt(Instant.now())
                .build();
    }

    public NotificationGetResponse newNotificationGetResponse(Notification notification) {
        var orderId = notification.getOrder() != null ? notification.getOrder().getId() : null;
        var compoundingId = notification.getCompounding() != null ? notification.getCompounding().getId() : null;
        return new NotificationGetResponse(
                notification.getId(),
                orderId,
                compoundingId,
                notification.getCustomer().getId(),
                notification.getCustomerPhone(),
                notification.getCustomerName(),
                notification.getMessage(),
                notification.getLink(),
                notification.getSentAt()
        );
    }
}
