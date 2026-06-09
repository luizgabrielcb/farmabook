package br.com.luizgabriel.farmabook.compounding.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CompoundingPostRequest(
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "customerId is required")
        UUID customerId,

        @NotNull(message = "pharmacyId is required")
        UUID pharmacyId,

        @DecimalMin(value = "0.0", message = "value must be non-negative")
        @DecimalMax(value = "100000.00", message = "value must be at most 100000")
        @Digits(integer = 6, fraction = 2, message = "value must have at most 2 decimal places")
        BigDecimal value,

        @Size(max = 500, message = "observations must be at most 500 characters")
        String observations
) {
}
