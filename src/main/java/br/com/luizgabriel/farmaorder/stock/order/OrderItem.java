package br.com.luizgabriel.farmaorder.stock.order;

import br.com.luizgabriel.farmaorder.shared.Auditable;
import br.com.luizgabriel.farmaorder.stock.Category;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE order_items SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class OrderItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 150)
    private String product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderItemStatus status;

    @Column(name = "ordered_by_id")
    private UUID orderedById;
    @Column(name = "ordered_by_name", length = 100)
    private String orderedByName;
    @Column(name = "ordered_at")
    private Instant orderedAt;

    @Column(name = "received_by_id")
    private UUID receivedById;
    @Column(name = "received_by_name", length = 100)
    private String receivedByName;
    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "delivered_by_id")
    private UUID deliveredById;
    @Column(name = "delivered_by_name", length = 100)
    private String deliveredByName;
    @Column(name = "delivered_at")
    private Instant deliveredAt;
}
