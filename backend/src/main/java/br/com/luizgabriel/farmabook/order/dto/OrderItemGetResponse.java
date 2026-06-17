package br.com.luizgabriel.farmabook.order.dto;

import br.com.luizgabriel.farmabook.catalog.Category;
import br.com.luizgabriel.farmabook.order.OrderItemStatus;
import br.com.luizgabriel.farmabook.order.OrderPaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderItemGetResponse(
        UUID id,
        String product,
        Category category,
        Integer quantity,
        OrderItemStatus status,

        UUID orderedById,
        String orderedByName,
        Instant orderedAt,

        UUID receivedById,
        String receivedByName,
        Instant receivedAt,

        UUID deliveredById,
        String deliveredByName,
        Instant deliveredAt,

        Instant createdAt,
        Instant updatedAt,

        UUID distributorId,
        String distributorName,
        BigDecimal price,
        OrderPaymentStatus paymentStatus,

        UUID paymentChangedById,
        String paymentChangedByName,
        Instant paymentChangedAt
) {
}
