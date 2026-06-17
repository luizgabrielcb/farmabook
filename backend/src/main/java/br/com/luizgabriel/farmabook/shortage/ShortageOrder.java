package br.com.luizgabriel.farmabook.shortage;

import br.com.luizgabriel.farmabook.shared.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shortage_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE shortage_orders SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ShortageOrder extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "shortage_type", nullable = false, length = 20)
    private ShortageType shortageType;

    @Column(name = "distributor_id", nullable = false, columnDefinition = "uuid")
    private UUID distributorId;

    @Column(name = "distributor_name", nullable = false, length = 100)
    private String distributorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShortageOrderStatus status = ShortageOrderStatus.PENDING;

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

    @Column(columnDefinition = "text")
    private String observations;
}
