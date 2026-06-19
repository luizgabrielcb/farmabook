package br.com.luizgabriel.farmabook.prescription;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Prescription> findWithItemsById(UUID id);
}
