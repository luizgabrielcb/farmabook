package br.com.luizgabriel.farmaorder.stock.shortage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShortageRepository extends JpaRepository<Shortage, UUID> {
}
