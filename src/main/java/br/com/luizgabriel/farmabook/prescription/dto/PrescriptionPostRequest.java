package br.com.luizgabriel.farmabook.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record PrescriptionPostRequest(
        UUID customerId,

        @NotEmpty(message = "prescription must have at least one item")
        @Valid
        List<PrescriptionItemPostRequest> items,

        String observations
) {
}
