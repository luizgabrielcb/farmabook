package br.com.luizgabriel.farmabook.prescription.dto;

import br.com.luizgabriel.farmabook.prescription.PrescriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record PrescriptionPutResponse(
        UUID id,
        UUID customerId,
        String customerName,
        PrescriptionStatus status,
        Instant updatedAt
) {
}
