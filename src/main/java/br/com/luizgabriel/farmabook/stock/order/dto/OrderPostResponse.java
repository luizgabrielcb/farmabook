package br.com.luizgabriel.farmabook.stock.order.dto;

import br.com.luizgabriel.farmabook.stock.order.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPostResponse(
        UUID id,
        UUID customerId,
        String customerName,
        OrderStatus status,
        UUID createdById,
        String createdByName,
        Instant createdAt,
        List<OrderItemGetResponse> items
) {
}
