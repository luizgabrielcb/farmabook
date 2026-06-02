package br.com.luizgabriel.farmaorder.stock.order.dto;

import br.com.luizgabriel.farmaorder.stock.Category;
import br.com.luizgabriel.farmaorder.stock.order.OrderItemStatus;

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
        Instant updatedAt
) {
}
