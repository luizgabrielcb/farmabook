package br.com.luizgabriel.farmabook.stock.shortage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShortageRepository extends JpaRepository<Shortage, UUID> {
}
