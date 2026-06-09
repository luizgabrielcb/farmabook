package br.com.luizgabriel.farmabook.stock.order.dto;

import br.com.luizgabriel.farmabook.stock.Category;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record OrderItemPutRequest(
        @NotBlank(message = "product is required")
        @Size(max = 150, message = "product must be at most 150 characters")
        String product,

        @NotNull(message = "category is required")
        Category category,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be positive")
        @Max(value = 1000, message = "quantity must be at most 1000")
        Integer quantity,

        @DecimalMin(value = "0.0", message = "price must be non-negative")
        @DecimalMax(value = "100000.00", message = "price must be at most 100000")
        @Digits(integer = 6, fraction = 2, message = "price must have at most 2 decimal places")
        BigDecimal price
) {
}
