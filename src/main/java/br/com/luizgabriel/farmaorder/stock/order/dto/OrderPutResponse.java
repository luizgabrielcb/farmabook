package br.com.luizgabriel.farmaorder.stock.order.dto;

import br.com.luizgabriel.farmaorder.stock.order.OrderStatus;

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
