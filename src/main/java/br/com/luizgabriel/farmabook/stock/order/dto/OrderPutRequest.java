package br.com.luizgabriel.farmabook.stock.order.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPutRequest(
        @NotNull(message = "customerId is required")
        UUID customerId,

        String observations,

        @DecimalMin(value = "0.0", message = "totalPrice must be non-negative")
        @DecimalMax(value = "100000.00", message = "totalPrice must be at most 100000")
        @Digits(integer = 6, fraction = 2, message = "totalPrice must have at most 2 decimal places")
        BigDecimal totalPrice
) {
}
