package br.com.luizgabriel.farmabook.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ValidatePinRequest(
        @NotBlank(message = "pin is required")
        @Pattern(regexp = "\\d{1,4}", message = "Pin must be 1 to 4 digits")
        String pin
) {
}

