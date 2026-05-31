package br.com.luizgabriel.farmaorder.stock.shortage.dto;

import br.com.luizgabriel.farmaorder.stock.Category;
import br.com.luizgabriel.farmaorder.stock.shortage.ShortageStatus;

import java.time.Instant;
import java.util.UUID;

public record ShortagePutResponse(
        UUID id,
        String product,
        Category category,
        Integer quantity,
        ShortageStatus status,
        Instant updatedAt
) {
}
