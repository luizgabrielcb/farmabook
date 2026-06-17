package br.com.luizgabriel.farmabook.compounding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompoundingRepository extends JpaRepository<Compounding, UUID> {
}
