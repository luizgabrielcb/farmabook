package br.com.luizgabriel.farmabook.prescription;

import br.com.luizgabriel.farmabook.shared.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prescription_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE prescription_items SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class PrescriptionItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(nullable = false, length = 150)
    private String product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 50)
    private String batch;

    @Column(nullable = false, length = 7)
    private String expiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PrescriptionItemStatus status = PrescriptionItemStatus.PENDING;

    @Column(name = "received_by_id")
    private UUID receivedById;

    @Column(name = "received_by_name", length = 100)
    private String receivedByName;

    @Column(name = "received_at")
    private Instant receivedAt;
}
