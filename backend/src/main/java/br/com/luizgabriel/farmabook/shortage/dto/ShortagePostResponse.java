package br.com.luizgabriel.farmabook.shortage.dto;

import br.com.luizgabriel.farmabook.catalog.Category;
import br.com.luizgabriel.farmabook.shortage.ShortageStatus;
import br.com.luizgabriel.farmabook.shortage.ShortageType;

import java.time.Instant;
import java.util.UUID;

public record ShortagePostResponse(
        UUID id,
        String product,
        Category category,
        Integer quantity,
        ShortageStatus status,
        ShortageType shortageType,
        UUID createdById,
        String createdByName,
        Instant createdAt
) {
}
