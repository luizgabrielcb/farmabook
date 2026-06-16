package br.com.luizgabriel.farmabook.compounding;

import br.com.luizgabriel.farmabook.shared.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compoundings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE compoundings SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Compounding extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "customer_id", nullable = false, columnDefinition = "uuid")
    private UUID customerId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "pharmacy_id", nullable = false, columnDefinition = "uuid")
    private UUID pharmacyId;

    @Column(name = "pharmacy_name", nullable = false, length = 150)
    private String pharmacyName;

    @Column(name = "pharmacy_city", nullable = false, length = 100)
    private String pharmacyCity;

    @Column(precision = 10, scale = 2)
    private BigDecimal value;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CompoundingStatus status = CompoundingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.TO_PAY;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "created_by_id", nullable = false, columnDefinition = "uuid")
    private UUID createdById;

    @Column(name = "created_by_name", nullable = false, length = 100)
    private String createdByName;

    @Column(name = "ordered_by_id", columnDefinition = "uuid")
    private UUID orderedById;

    @Column(name = "ordered_by_name", length = 100)
    private String orderedByName;

    @Column(name = "ordered_at")
    private Instant orderedAt;

    @Column(name = "received_by_id", columnDefinition = "uuid")
    private UUID receivedById;

    @Column(name = "received_by_name", length = 100)
    private String receivedByName;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "delivered_by_id", columnDefinition = "uuid")
    private UUID deliveredById;

    @Column(name = "delivered_by_name", length = 100)
    private String deliveredByName;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "payment_changed_by_id", columnDefinition = "uuid")
    private UUID paymentChangedById;

    @Column(name = "payment_changed_by_name", length = 100)
    private String paymentChangedByName;

    @Column(name = "payment_changed_at")
    private Instant paymentChangedAt;
}
