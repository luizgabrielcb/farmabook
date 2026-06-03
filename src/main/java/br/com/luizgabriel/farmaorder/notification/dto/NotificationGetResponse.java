package br.com.luizgabriel.farmaorder.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationGetResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        String customerPhone,
        String customerName,
        String message,
        String link,
        Instant sentAt
) {
}
