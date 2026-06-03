package br.com.luizgabriel.farmaorder.customer;

import br.com.luizgabriel.farmaorder.commons.CustomerUtils;
import br.com.luizgabriel.farmaorder.exception.ConflictException;
import br.com.luizgabriel.farmaorder.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @InjectMocks
    private CustomerService service;

    @InjectMocks
    private CustomerUtils utils;

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerMapper mapper;

    // --- save ---

    @Test
    @DisplayName("save should return CustomerPostResponse when successful")
    void save_ReturnsCustomerPostResponse_WhenSuccessful() {
        var request = utils.newCustomerPostRequest();
        var customer = utils.newCustomer();
        var response = utils.newCustomerPostResponse(customer);

        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.empty());
        BDDMockito.when(repository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.empty());
        BDDMockito.when(mapper.toCustomer(request)).thenReturn(customer);
        BDDMockito.when(repository.save(customer)).thenReturn(customer);
        BDDMockito.when(mapper.toCustomerPostResponse(customer)).thenReturn(response);

        var result = service.save(request);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(customer);
    }

    @Test
    @DisplayName("save should skip phone validation and return CustomerPostResponse when phone number is null")
    void save_ReturnsCustomerPostResponse_WhenPhoneNumberIsNull() {
        var request = utils.newCustomerPostRequestWithoutPhone();
        var customer = utils.newCustomerWithoutPhone();
        var response = utils.newCustomerPostResponse(customer);

        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.empty());
        // findByPhoneNumber not stubbed — must not be called when phoneNumber is null
        BDDMockito.when(mapper.toCustomer(request)).thenReturn(customer);
        BDDMockito.when(repository.save(customer)).thenReturn(customer);
        BDDMockito.when(mapper.toCustomerPostResponse(customer)).thenReturn(response);

        var result = service.save(request);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(customer);
        BDDMockito.then(repository).should(Mockito.never()).findByPhoneNumber(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("save should throw ConflictException when name already exists")
    void save_ThrowsConflictException_WhenNameAlreadyExists() {
        var request = utils.newCustomerPostRequest();
        var existingCustomer = utils.newCustomer();

        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.of(existingCustomer));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Customer.class));
    }

    @Test
    @DisplayName("save should throw ConflictException when phone number is already in use")
    void save_ThrowsConflictException_WhenPhoneNumberAlreadyInUse() {
        var request = utils.newCustomerPostRequest();
        var existingCustomer = utils.newOtherCustomer();

        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.empty());
        BDDMockito.when(repository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.of(existingCustomer));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Customer.class));
    }

    // --- findAll ---

    @Test
    @DisplayName("findAll should return a page of customers when successful")
    void findAll_ReturnsPageOfCustomers_WhenSuccessful() {
        var pageable = Pageable.ofSize(10);
        var customer = utils.newCustomer();
        var response = utils.newCustomerGetResponse(customer);
        var page = new PageImpl<>(List.of(customer));

        BDDMockito.when(repository.findAll(pageable)).thenReturn(page);
        BDDMockito.when(mapper.toCustomerGetResponse(customer)).thenReturn(response);

        var result = service.findAll(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("findAll should return an empty page when no customers exist")
    void findAll_ReturnsEmptyPage_WhenNoCustomersExist() {
        var pageable = Pageable.ofSize(10);

        BDDMockito.when(repository.findAll(pageable)).thenReturn(Page.empty());

        var result = service.findAll(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // --- findById ---

    @Test
    @DisplayName("findById should return CustomerGetResponse when customer is found")
    void findById_ReturnsCustomerGetResponse_WhenSuccessful() {
        var customer = utils.newCustomer();
        var response = utils.newCustomerGetResponse(customer);

        BDDMockito.when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));
        BDDMockito.when(mapper.toCustomerGetResponse(customer)).thenReturn(response);

        var result = service.findById(customer.getId());

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("findById should throw NotFoundException when customer is not found")
    void findById_ThrowsNotFoundException_WhenCustomerNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    // --- update ---

    @Test
    @DisplayName("update should return CustomerPutResponse when successful")
    void update_ReturnsCustomerPutResponse_WhenSuccessful() {
        var customer = utils.newCustomer();
        var request = utils.newCustomerPutRequest();
        var response = utils.newCustomerPutResponse(customer);

        BDDMockito.when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));
        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.empty());
        BDDMockito.when(repository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.empty());
        BDDMockito.when(repository.save(customer)).thenReturn(customer);
        BDDMockito.when(mapper.toCustomerPutResponse(customer)).thenReturn(response);

        var result = service.update(customer.getId(), request);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(customer);
    }

    @Test
    @DisplayName("update should throw NotFoundException when customer is not found")
    void update_ThrowsNotFoundException_WhenCustomerNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newCustomerPutRequest();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Customer.class));
    }

    @Test
    @DisplayName("update should throw ConflictException when name is already in use by another customer")
    void update_ThrowsConflictException_WhenNameAlreadyInUseByAnotherCustomer() {
        var customer = utils.newCustomer();
        var anotherCustomer = utils.newOtherCustomer();
        var request = utils.newCustomerPutRequest();

        BDDMockito.when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));
        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.of(anotherCustomer));

        assertThatThrownBy(() -> service.update(customer.getId(), request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Customer.class));
    }

    @Test
    @DisplayName("update should not throw when name belongs to the same customer being updated")
    void update_DoesNotThrow_WhenNameBelongsToSameCustomer() {
        var customer = utils.newCustomer();
        var request = utils.newCustomerPutRequest();
        var response = utils.newCustomerPutResponse(customer);

        BDDMockito.when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));
        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.of(customer));
        BDDMockito.when(repository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.empty());
        BDDMockito.when(repository.save(customer)).thenReturn(customer);
        BDDMockito.when(mapper.toCustomerPutResponse(customer)).thenReturn(response);

        assertThatNoException().isThrownBy(() -> service.update(customer.getId(), request));

        BDDMockito.then(repository).should().save(customer);
    }

    @Test
    @DisplayName("update should throw ConflictException when phone number is already in use by another customer")
    void update_ThrowsConflictException_WhenPhoneNumberAlreadyInUseByAnotherCustomer() {
        var customer = utils.newCustomer();
        var anotherCustomer = utils.newOtherCustomer();
        var request = utils.newCustomerPutRequest();

        BDDMockito.when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));
        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.empty());
        BDDMockito.when(repository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.of(anotherCustomer));

        assertThatThrownBy(() -> service.update(customer.getId(), request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Customer.class));
    }

    @Test
    @DisplayName("update should not throw when phone number belongs to the same customer being updated")
    void update_DoesNotThrow_WhenPhoneNumberBelongsToSameCustomer() {
        var customer = utils.newCustomer();
        var request = utils.newCustomerPutRequest();
        var response = utils.newCustomerPutResponse(customer);

        BDDMockito.when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));
        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.empty());
        BDDMockito.when(repository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.of(customer));
        BDDMockito.when(repository.save(customer)).thenReturn(customer);
        BDDMockito.when(mapper.toCustomerPutResponse(customer)).thenReturn(response);

        assertThatNoException().isThrownBy(() -> service.update(customer.getId(), request));

        BDDMockito.then(repository).should().save(customer);
    }

    @Test
    @DisplayName("update should skip phone validation and save when phone number is null")
    void update_DoesNotCallFindByPhoneNumber_WhenPhoneNumberIsNull() {
        var customer = utils.newCustomer();
        var request = utils.newCustomerPutRequestWithoutPhone();
        var response = utils.newCustomerPutResponse(customer);

        BDDMockito.when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));
        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.empty());
        // findByPhoneNumber not stubbed — must not be called when phoneNumber is null
        BDDMockito.when(repository.save(customer)).thenReturn(customer);
        BDDMockito.when(mapper.toCustomerPutResponse(customer)).thenReturn(response);

        assertThatNoException().isThrownBy(() -> service.update(customer.getId(), request));

        BDDMockito.then(repository).should().save(customer);
        BDDMockito.then(repository).should(Mockito.never()).findByPhoneNumber(ArgumentMatchers.any());
    }

    // --- delete ---

    @Test
    @DisplayName("delete should delete the customer when successful")
    void delete_DeletesCustomer_WhenSuccessful() {
        var customer = utils.newCustomer();

        BDDMockito.when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));

        service.delete(customer.getId());

        BDDMockito.then(repository).should().delete(customer);
    }

    @Test
    @DisplayName("delete should throw NotFoundException when customer is not found")
    void delete_ThrowsNotFoundException_WhenCustomerNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(Customer.class));
    }
}
