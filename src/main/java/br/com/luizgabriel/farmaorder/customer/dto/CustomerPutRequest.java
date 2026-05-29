package br.com.luizgabriel.farmaorder.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerPutRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Size(max = 20, message = "phoneNumber must be at most 20 characters")
        String phoneNumber
) {
}
