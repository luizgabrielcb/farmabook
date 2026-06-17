package br.com.luizgabriel.farmabook.compounding.pharmacy.dto;

import java.time.Instant;
import java.util.UUID;

public record CompoundingPharmacyGetResponse(
        UUID id,
        String name,
        String city,
        Instant createdAt
) {
}
