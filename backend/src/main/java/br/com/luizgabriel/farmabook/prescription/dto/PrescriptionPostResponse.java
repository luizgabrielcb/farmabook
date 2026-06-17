package br.com.luizgabriel.farmabook.prescription.dto;

import br.com.luizgabriel.farmabook.prescription.PrescriptionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrescriptionPostResponse(
        UUID id,
        UUID customerId,
        String customerName,
        PrescriptionStatus status,
        UUID createdById,
        String createdByName,
        String observations,
        Instant createdAt,
        List<PrescriptionItemGetResponse> items
) {
}
