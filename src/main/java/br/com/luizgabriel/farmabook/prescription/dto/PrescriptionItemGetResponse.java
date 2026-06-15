package br.com.luizgabriel.farmabook.prescription.dto;

import br.com.luizgabriel.farmabook.prescription.PrescriptionItemStatus;

import java.time.Instant;
import java.util.UUID;

public record PrescriptionItemGetResponse(
        UUID id,
        String product,
        Integer quantity,
        String batch,
        String expiry,
        PrescriptionItemStatus status,
        UUID receivedById,
        String receivedByName,
        Instant receivedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
