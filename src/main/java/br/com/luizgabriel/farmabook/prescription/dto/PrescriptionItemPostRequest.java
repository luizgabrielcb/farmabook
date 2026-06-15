package br.com.luizgabriel.farmabook.prescription.dto;

import jakarta.validation.constraints.*;

public record PrescriptionItemPostRequest(
        @NotBlank(message = "product is required")
        @Size(max = 150, message = "product must be at most 150 characters")
        String product,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be positive")
        @Max(value = 999, message = "quantity must be at most 999")
        Integer quantity,

        @NotBlank(message = "batch is required")
        @Size(max = 50, message = "batch must be at most 50 characters")
        String batch,

        @NotBlank(message = "expiry is required")
        @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{4}$", message = "expiry must be in MM/yyyy format")
        String expiry
) {
}
