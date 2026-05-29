package br.com.luizgabriel.farmaorder.customer.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerPutResponse(
        UUID id,
        String name,
        String phoneNumber,
        Instant updatedAt
) {
}
