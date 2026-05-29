package br.com.luizgabriel.farmaorder.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePinRequest(
        @NotBlank(message = "currentPin is required")
        @Pattern(regexp = "\\d{1,4}", message = "Current pin must be 1 to 4 digits")
        String currentPin,

        @NotBlank(message = "newPin is required")
        @Pattern(regexp = "\\d{1,4}", message = "New pin must be 1 to 4 digits")
        String newPin
) {
}
