package br.com.luizgabriel.farmabook.distributor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface DistributorRepository extends JpaRepository<Distributor, UUID> {}
