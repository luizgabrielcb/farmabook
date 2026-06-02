package br.com.luizgabriel.farmaorder.stock.order.dto;

import br.com.luizgabriel.farmaorder.stock.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OrderItemPutRequest(
        @NotBlank(message = "product is required")
        @Size(max = 150, message = "product must be at most 150 characters")
        String product,

        @NotNull(message = "category is required")
        Category category,

        @Positive(message = "quantity must be positive when informed")
        Integer quantity
) {
}
