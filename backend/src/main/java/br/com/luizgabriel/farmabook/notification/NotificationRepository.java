package br.com.luizgabriel.farmabook.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findAllByOrderId(UUID orderId, Pageable pageable);

    Page<Notification> findAllByCompoundingId(UUID compoundingId, Pageable pageable);
}
