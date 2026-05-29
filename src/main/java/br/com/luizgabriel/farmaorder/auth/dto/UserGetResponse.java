package br.com.luizgabriel.farmaorder.auth.dto;

import br.com.luizgabriel.farmaorder.auth.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserGetResponse(
        UUID id,
        String name,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
