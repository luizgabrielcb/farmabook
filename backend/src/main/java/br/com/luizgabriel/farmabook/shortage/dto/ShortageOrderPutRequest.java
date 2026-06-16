package br.com.luizgabriel.farmabook.shortage.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ShortageOrderPutRequest(
        @NotNull(message = "distributorId is required")
        UUID distributorId,

        String observations
) {
}
