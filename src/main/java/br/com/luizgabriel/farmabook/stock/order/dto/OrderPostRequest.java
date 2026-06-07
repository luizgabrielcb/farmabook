package br.com.luizgabriel.farmabook.stock.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OrderPostRequest(
        @NotNull(message = "customerId is required")
        UUID customerId,

        @NotEmpty(message = "order must have at least one item")
        @Valid
        List<OrderItemPostRequest> items) {
}
