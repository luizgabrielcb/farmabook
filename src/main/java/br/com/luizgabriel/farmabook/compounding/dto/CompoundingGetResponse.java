package br.com.luizgabriel.farmabook.compounding.dto;

import br.com.luizgabriel.farmabook.compounding.CompoundingStatus;
import br.com.luizgabriel.farmabook.compounding.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CompoundingGetResponse(
        UUID id,
        Integer quantity,
        UUID customerId,
        String customerName,
        UUID pharmacyId,
        String pharmacyName,
        String pharmacyCity,
        BigDecimal value,
        String observations,
        CompoundingStatus status,
        PaymentStatus paymentStatus,
        Instant notifiedAt,
        UUID createdById,
        String createdByName,
        UUID orderedById,
        String orderedByName,
        Instant orderedAt,
        UUID receivedById,
        String receivedByName,
        Instant receivedAt,
        UUID deliveredById,
        String deliveredByName,
        Instant deliveredAt,
        Instant createdAt,
        Instant updatedAt
) {
}
