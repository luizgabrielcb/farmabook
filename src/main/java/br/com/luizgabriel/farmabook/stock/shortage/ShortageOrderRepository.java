package br.com.luizgabriel.farmabook.stock.shortage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShortageOrderRepository extends JpaRepository<ShortageOrder, UUID> {
    Page<ShortageOrder> findByShortageType(ShortageType shortageType, Pageable pageable);
    Page<ShortageOrder> findByShortageTypeAndDistributorId(ShortageType shortageType, UUID distributorId, Pageable pageable);
}
