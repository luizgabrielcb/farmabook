package br.com.luizgabriel.farmabook.auth.dto;

import br.com.luizgabriel.farmabook.auth.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserPostResponse(
        UUID id,
        String name,
        UserRole role,
        boolean active,
        Instant createdAt
) {
}
