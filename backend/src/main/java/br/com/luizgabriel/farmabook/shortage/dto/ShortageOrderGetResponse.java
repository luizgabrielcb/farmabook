package br.com.luizgabriel.farmabook.shortage.dto;

import br.com.luizgabriel.farmabook.shortage.ShortageOrderStatus;
import br.com.luizgabriel.farmabook.shortage.ShortageType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShortageOrderGetResponse(
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
        List<ShortageGetResponse> shortages,
        Instant createdAt,
        Instant updatedAt
) {
}
