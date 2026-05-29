package br.com.luizgabriel.farmaorder.customer;

import br.com.luizgabriel.farmaorder.customer.dto.*;
import br.com.luizgabriel.farmaorder.exception.ConflictException;
import br.com.luizgabriel.farmaorder.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public CustomerPostResponse save(CustomerPostRequest customerPostRequest) {
        validateNameAlreadyExists(customerPostRequest.name());

        if (customerPostRequest.phoneNumber() != null) {
            validatePhoneNumberAlreadyInUse(customerPostRequest.phoneNumber());
        }

        var customer = mapper.toCustomer(customerPostRequest);

        var savedCustomer = repository.save(customer);

        return mapper.toCustomerPostResponse(savedCustomer);
    }

    public Page<CustomerGetResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toCustomerGetResponse);
    }

    public CustomerGetResponse findById(UUID id) {
        var customer = findByIdOrThrowNotFoundException(id);

        return mapper.toCustomerGetResponse(customer);
    }

    public CustomerPutResponse update(UUID id, CustomerPutRequest customerPutRequest) {
        var customer = findByIdOrThrowNotFoundException(id);

        validateNameAlreadyExists(customerPutRequest.name());
        customer.setName(customerPutRequest.name());

        if (customerPutRequest.phoneNumber() != null) {
            validatePhoneNumberAlreadyInUse(customerPutRequest.phoneNumber(), id);
            customer.setPhoneNumber(customerPutRequest.phoneNumber());
        }

        var updatedCustomer = repository.save(customer);

        return mapper.toCustomerPutResponse(updatedCustomer);
    }

    public void delete(UUID id) {
        var customer = findByIdOrThrowNotFoundException(id);

        repository.delete(customer);
    }

    private Customer findByIdOrThrowNotFoundException(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer with id '" + id + "' not found"));
    }

    private void validateNameAlreadyExists(String name) {
        repository.findByNameIgnoreCase(name)
                .ifPresent(c -> {
                    throw new ConflictException("Customer with name '" + name + "' already exists");
                });
    }

    public void validatePhoneNumberAlreadyInUse(String phoneNumber) {
        repository.findByPhoneNumber(phoneNumber)
                .ifPresent(c -> {
                    throw new ConflictException(
                            "The phone number '" + phoneNumber + "' is already in use by customer '" + c.getName() + "'");
                });
    }

    public void validatePhoneNumberAlreadyInUse(String phoneNumber, UUID customerId) {
        repository.findByPhoneNumber(phoneNumber)
                .filter(c -> !c.getId().equals(customerId))
                .ifPresent(c -> {
                    throw new ConflictException("The phone number '" + phoneNumber + "' is already in use by customer '" + c.getName() + "'");
                });
    }
}
