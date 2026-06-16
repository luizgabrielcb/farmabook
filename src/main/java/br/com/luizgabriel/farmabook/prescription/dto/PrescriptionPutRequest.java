package br.com.luizgabriel.farmabook.prescription.dto;

import java.util.UUID;

public record PrescriptionPutRequest(
        UUID customerId,

        String observations
) {
}
