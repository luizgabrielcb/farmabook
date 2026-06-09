package br.com.luizgabriel.farmabook.compounding.pharmacy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompoundingPharmacyRepository extends JpaRepository<CompoundingPharmacy, UUID> {
}
