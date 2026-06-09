package br.com.luizgabriel.farmabook.compounding.pharmacy.dto;

import java.time.Instant;
import java.util.UUID;

public record CompoundingPharmacyPutResponse(
        UUID id,
        String name,
        String city,
        Instant updatedAt
) {
}
