package br.com.luizgabriel.farmabook.stock.shortage.dto;

import br.com.luizgabriel.farmabook.stock.shortage.ShortageOrderStatus;
import br.com.luizgabriel.farmabook.stock.shortage.ShortageType;

import java.time.Instant;
import java.util.UUID;

public record ShortageOrderListResponse(
        UUID id,
        ShortageType shortageType,
        UUID distributorId,
        String distributorName,
        ShortageOrderStatus status,
        UUID createdById,
        String createdByName,
        UUID orderedById,
        String orderedByName,
        Instant orderedAt,
        String observations,
        Instant createdAt,
        Instant updatedAt
) {
}
