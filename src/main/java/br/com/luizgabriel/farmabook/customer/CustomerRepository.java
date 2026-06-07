package br.com.luizgabriel.farmabook.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByNameIgnoreCase(String name);

    Optional<Customer> findByPhoneNumber(String phoneNumber);
}
