package br.com.luizgabriel.farmaorder.auth.dto;

import br.com.luizgabriel.farmaorder.auth.UserRole;

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
