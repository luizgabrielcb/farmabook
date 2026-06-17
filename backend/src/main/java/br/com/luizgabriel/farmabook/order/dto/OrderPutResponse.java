package br.com.luizgabriel.farmabook.order.dto;

import br.com.luizgabriel.farmabook.order.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderPutResponse(
        UUID id,
        UUID customerId,
        String customerName,
        OrderStatus status,
        Instant updatedAt
) {
}
