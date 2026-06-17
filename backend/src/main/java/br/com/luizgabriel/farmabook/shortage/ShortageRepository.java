package br.com.luizgabriel.farmabook.shortage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShortageRepository extends JpaRepository<Shortage, UUID> {
    List<Shortage> findAllByShortageOrderId(UUID shortageOrderId);
}
