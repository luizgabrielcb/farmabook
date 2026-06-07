package br.com.luizgabriel.farmabook.stock.order.dto;

import br.com.luizgabriel.farmabook.stock.order.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderGetResponse(
        UUID id,
        UUID customerId,
        String customerName,
        OrderStatus status,
        Instant notifiedAt,
        UUID createdById,
        String createdByName,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemGetResponse> items
) {
}
