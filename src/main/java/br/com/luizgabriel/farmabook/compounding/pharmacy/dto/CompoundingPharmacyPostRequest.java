package br.com.luizgabriel.farmabook.compounding.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;

public record CompoundingPharmacyPostRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "city is required")
        String city
) {
}
