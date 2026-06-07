package br.com.luizgabriel.farmabook.notification;

import br.com.luizgabriel.farmabook.customer.CustomerService;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import br.com.luizgabriel.farmabook.notification.dto.NotificationGetResponse;
import br.com.luizgabriel.farmabook.stock.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final ZoneId STORE_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String WA_ME_BASE = "https://wa.me/";

    private final NotificationRepository notificationRepository;
    private final CustomerService customerService;
    private final NotificationMapper notificationMapper;

    @Transactional
    public NotificationGetResponse generateForOrderReceived(Order order) {
        Notification saved = persistNotification(order);
        return notificationMapper.toNotificationGetResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<NotificationGetResponse> findAllByOrderId(UUID orderId, Pageable pageable) {
        return notificationRepository.findAllByOrderId(orderId, pageable)
                .map(notificationMapper::toNotificationGetResponse);
    }

    @Transactional
    public NotificationGetResponse resend(UUID notificationId) {
        var original = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        var saved = persistNotification(original.getOrder());

        return notificationMapper.toNotificationGetResponse(saved);
    }

    private Notification persistNotification(Order order) {
        var customer = customerService.findByIdOrThrowNotFound(order.getCustomerId());

        var phone = sanitizePhone(customer.getPhoneNumber());
        var message = buildMessage(customer.getName());
        var link = buildWaMeLink(phone, message);

        var notification = new Notification();

        notification.setOrder(order);
        notification.setCustomer(customer);
        notification.setCustomerPhone(phone);
        notification.setCustomerName(customer.getName());
        notification.setMessage(message);
        notification.setLink(link);
        notification.setSentAt(Instant.now());

        return notificationRepository.save(notification);
    }

    private String buildMessage(String fullName) {
        return String.format(
                "%s, %s! Tudo bem? A sua encomenda acabou de chegar aqui na farmácia.",
                resolveGreeting(),
                shortenName(fullName)
        );
    }

    private String resolveGreeting() {
        int hour = LocalTime.now(STORE_ZONE).getHour();
        if (hour >= 5 && hour < 12) return "Bom dia";
        if (hour >= 12 && hour < 18) return "Boa tarde";
        return "Boa noite";
    }

    private String shortenName(String fullName) {
        var parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + parts[1];
    }

    private String sanitizePhone(String phone) {
        var digits = phone.replaceAll("\\D", "");
        if (!digits.startsWith("55")) {
            digits = "55" + digits;
        }
        return digits;
    }

    private String buildWaMeLink(String phone, String message) {
        var encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return WA_ME_BASE + phone + "?text=" + encoded;
    }
}
