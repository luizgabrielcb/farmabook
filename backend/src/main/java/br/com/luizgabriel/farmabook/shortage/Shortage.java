package br.com.luizgabriel.farmabook.shortage;

import br.com.luizgabriel.farmabook.shared.Auditable;
import br.com.luizgabriel.farmabook.catalog.Category;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shortages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE shortages SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Shortage extends Auditable {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 150)
    private String product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "shortage_type", nullable = false, length = 20)
    private ShortageType shortageType;

    @Column
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShortageStatus status = ShortageStatus.PENDING;

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

    @Column(name = "shortage_order_id", columnDefinition = "uuid")
    private UUID shortageOrderId;

    @Column(name = "cost_price", precision = 8, scale = 2)
    private java.math.BigDecimal costPrice;
}
