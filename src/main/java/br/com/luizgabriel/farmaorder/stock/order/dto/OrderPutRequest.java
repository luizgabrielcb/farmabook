package br.com.luizgabriel.farmaorder.stock.order.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderPutRequest(
        @NotNull(message = "customerId is required")
        UUID customerId
) {
}
