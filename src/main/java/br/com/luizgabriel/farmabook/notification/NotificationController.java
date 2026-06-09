package br.com.luizgabriel.farmabook.notification;

import br.com.luizgabriel.farmabook.notification.dto.NotificationGetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping("/orders/{orderId}/notifications")
    public ResponseEntity<Page<NotificationGetResponse>> findAllByOrderId(@PathVariable UUID orderId, Pageable pageable) {
        var notificationGetResponsePage = service.findAllByOrderId(orderId, pageable);

        return ResponseEntity.ok(notificationGetResponsePage);
    }

    @GetMapping("/notifications/compoundings/{compoundingId}")
    public ResponseEntity<Page<NotificationGetResponse>> findAllByCompoundingId(@PathVariable UUID compoundingId, Pageable pageable) {
        var notificationGetResponsePage = service.findAllByCompoundingId(compoundingId, pageable);

        return ResponseEntity.ok(notificationGetResponsePage);
    }

    @PostMapping("/notifications/{id}/resend")
    public ResponseEntity<NotificationGetResponse> resend(@PathVariable UUID id) {
        var notificationGetResponse = service.resend(id);

        return ResponseEntity.ok(notificationGetResponse);
    }
}
