package br.com.luizgabriel.farmabook.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {

    @Query("SELECT COUNT(i) FROM PrescriptionItem i WHERE i.prescription.id = :prescriptionId")
    long countActiveByPrescriptionId(@Param("prescriptionId") UUID prescriptionId);
}
