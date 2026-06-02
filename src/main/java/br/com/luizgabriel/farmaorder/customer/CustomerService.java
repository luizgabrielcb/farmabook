package br.com.luizgabriel.farmaorder.customer;

import br.com.luizgabriel.farmaorder.customer.dto.*;
import br.com.luizgabriel.farmaorder.exception.ConflictException;
import br.com.luizgabriel.farmaorder.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    @Transactional
    public CustomerPostResponse save(CustomerPostRequest customerPostRequest) {
        validateNameAlreadyExists(customerPostRequest.name());
        validatePhoneNumberAlreadyInUse(customerPostRequest.phoneNumber());

        var customer = mapper.toCustomer(customerPostRequest);

        var savedCustomer = repository.save(customer);

        return mapper.toCustomerPostResponse(savedCustomer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerGetResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toCustomerGetResponse);
    }

    @Transactional(readOnly = true)
    public CustomerGetResponse findById(UUID id) {
        var customer = findByIdOrThrowNotFound(id);

        return mapper.toCustomerGetResponse(customer);
    }

    @Transactional
    public CustomerPutResponse update(UUID id, CustomerPutRequest customerPutRequest) {
        var customer = findByIdOrThrowNotFound(id);

        validateNameAlreadyExists(customerPutRequest.name(), id);
        validatePhoneNumberAlreadyInUse(customerPutRequest.phoneNumber(), id);

        customer.setName(customerPutRequest.name());
        customer.setPhoneNumber(customerPutRequest.phoneNumber());

        var updatedCustomer = repository.save(customer);

        return mapper.toCustomerPutResponse(updatedCustomer);
    }

    @Transactional
    public void delete(UUID id) {
        var customer = findByIdOrThrowNotFound(id);

        repository.delete(customer);
    }

    public Customer findByIdOrThrowNotFound(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer with id '" + id + "' not found"));
    }

    private void validateNameAlreadyExists(String name) {
        repository.findByNameIgnoreCase(name)
                .ifPresent(c -> {
                    throw new ConflictException("Customer with name '" + name + "' already exists");
                });
    }

    private void validateNameAlreadyExists(String name, UUID customerId) {
        repository.findByNameIgnoreCase(name)
                .filter(c -> !c.getId().equals(customerId))
                .ifPresent(c -> {
                    throw new ConflictException("Customer with name '" + name + "' already exists");
                });
    }

    private void validatePhoneNumberAlreadyInUse(String phoneNumber) {
        if (phoneNumber == null) return;
        repository.findByPhoneNumber(phoneNumber)
                .ifPresent(c -> {
                    throw new ConflictException(
                            "The phone number '" + phoneNumber + "' is already in use by customer '" + c.getName() + "'");
                });
    }

    private void validatePhoneNumberAlreadyInUse(String phoneNumber, UUID customerId) {
        if (phoneNumber == null) return;
        repository.findByPhoneNumber(phoneNumber)
                .filter(c -> !c.getId().equals(customerId))
                .ifPresent(c -> {
                    throw new ConflictException("The phone number '" + phoneNumber + "' is already in use by customer '" + c.getName() + "'");
                });
    }
}
