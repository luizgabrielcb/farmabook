package br.com.luizgabriel.farmabook.customer.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerGetResponse(
        UUID id,
        String name,
        String phoneNumber,
        Instant createdAt,
        Instant updatedAt
) {
}
