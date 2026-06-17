package br.com.luizgabriel.farmabook.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("SELECT COUNT(i) FROM OrderItem i WHERE i.order.id = :orderId")
    long countActiveByOrderId(@Param("orderId") UUID orderId);
}
