package br.com.luizgabriel.farmabook.auth.dto;

import br.com.luizgabriel.farmabook.auth.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserPutRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotNull(message = "role is required")
        UserRole role
) {
}
