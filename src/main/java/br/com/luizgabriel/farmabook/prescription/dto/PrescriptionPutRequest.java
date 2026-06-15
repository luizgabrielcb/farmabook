package br.com.luizgabriel.farmabook.prescription.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PrescriptionPutRequest(
        @NotNull(message = "customerId is required")
        UUID customerId,

        String observations
) {
}
