package br.com.luizgabriel.farmabook.compounding.dto;

import br.com.luizgabriel.farmabook.compounding.CompoundingStatus;
import br.com.luizgabriel.farmabook.compounding.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record CompoundingPostResponse(
        UUID id,
        Integer quantity,
        String customerName,
        String pharmacyName,
        CompoundingStatus status,
        PaymentStatus paymentStatus,
        Instant createdAt
) {
}
