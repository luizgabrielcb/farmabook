package br.com.luizgabriel.farmabook.stock.shortage.dto;

import br.com.luizgabriel.farmabook.stock.shortage.ShortageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ShortageOrderPostRequest(
        @NotNull(message = "shortageType is required")
        ShortageType shortageType,

        @NotNull(message = "distributorId is required")
        UUID distributorId,

        @NotEmpty(message = "at least one item is required")
        @Valid
        List<ShortageOrderItemRequest> items,

        String observations
) {
}
