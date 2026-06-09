package br.com.luizgabriel.farmabook.compounding;

import br.com.luizgabriel.farmabook.commons.CompoundingPharmacyUtils;
import br.com.luizgabriel.farmabook.commons.CompoundingUtils;
import br.com.luizgabriel.farmabook.commons.CustomerUtils;
import br.com.luizgabriel.farmabook.commons.UserUtils;
import br.com.luizgabriel.farmabook.compounding.pharmacy.CompoundingPharmacyService;
import br.com.luizgabriel.farmabook.customer.CustomerService;
import br.com.luizgabriel.farmabook.exception.ConflictException;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import br.com.luizgabriel.farmabook.notification.NotificationService;
import br.com.luizgabriel.farmabook.notification.dto.NotificationGetResponse;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CompoundingServiceTest {

    @InjectMocks
    private CompoundingService service;

    @InjectMocks
    private CompoundingUtils utils;

    @InjectMocks
    private CompoundingPharmacyUtils pharmacyUtils;

    @InjectMocks
    private CustomerUtils customerUtils;

    @InjectMocks
    private UserUtils userUtils;

    @Mock
    private CompoundingRepository repository;

    @Mock
    private CompoundingMapper mapper;

    @Mock
    private CustomerService customerService;

    @Mock
    private CompoundingPharmacyService pharmacyService;

    @Mock
    private NotificationService notificationService;

    // ---- findAll ----

    @Test
    @DisplayName("findAll should return a page of compoundings when successful")
    void findAll_ReturnsPageOfCompoundings_WhenSuccessful() {
        var pageable = Pageable.ofSize(10);
        var compounding = utils.newCompounding();
        var response = utils.newCompoundingGetResponse(compounding);
        var page = new PageImpl<>(List.of(compounding));

        BDDMockito.when(repository.findAll(pageable)).thenReturn(page);
        BDDMockito.when(mapper.toCompoundingGetResponse(compounding)).thenReturn(response);

        var result = service.findAll(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("findAll should return empty page when no compoundings exist")
    void findAll_ReturnsEmptyPage_WhenNoCompoundingsExist() {
        var pageable = Pageable.ofSize(10);

        BDDMockito.when(repository.findAll(pageable)).thenReturn(Page.empty());

        var result = service.findAll(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // ---- findById ----

    @Test
    @DisplayName("findById should return CompoundingGetResponse when found")
    void findById_ReturnsGetResponse_WhenSuccessful() {
        var compounding = utils.newCompounding();
        var response = utils.newCompoundingGetResponse(compounding);

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));
        BDDMockito.when(mapper.toCompoundingGetResponse(compounding)).thenReturn(response);

        var result = service.findById(compounding.getId());

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("findById should throw NotFoundException when compounding is not found")
    void findById_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- save ----

    @Test
    @DisplayName("save should return CompoundingPostResponse when successful")
    void save_ReturnsPostResponse_WhenSuccessful() {
        var actor = userUtils.newUser();
        var request = utils.newCompoundingPostRequest();
        var customer = customerUtils.newCustomer();
        var pharmacy = pharmacyUtils.newPharmacy();
        var compounding = utils.newCompounding();
        var response = utils.newCompoundingPostResponse(compounding);

        BDDMockito.when(customerService.findByIdOrThrowNotFound(request.customerId())).thenReturn(customer);
        BDDMockito.when(pharmacyService.findByIdOrThrowNotFound(request.pharmacyId())).thenReturn(pharmacy);
        BDDMockito.when(mapper.toCompounding(request, customer, pharmacy, actor)).thenReturn(compounding);
        BDDMockito.when(repository.save(compounding)).thenReturn(compounding);
        BDDMockito.when(mapper.toCompoundingPostResponse(compounding)).thenReturn(response);

        var result = service.save(request, actor);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(compounding);
    }

    @Test
    @DisplayName("save should throw NotFoundException when customer is not found")
    void save_ThrowsNotFoundException_WhenCustomerNotFound() {
        var actor = userUtils.newUser();
        var request = utils.newCompoundingPostRequest();

        BDDMockito.when(customerService.findByIdOrThrowNotFound(request.customerId()))
                .thenThrow(new NotFoundException("Customer not found"));

        assertThatThrownBy(() -> service.save(request, actor))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    @Test
    @DisplayName("save should throw NotFoundException when pharmacy is not found")
    void save_ThrowsNotFoundException_WhenPharmacyNotFound() {
        var actor = userUtils.newUser();
        var request = utils.newCompoundingPostRequest();
        var customer = customerUtils.newCustomer();

        BDDMockito.when(customerService.findByIdOrThrowNotFound(request.customerId())).thenReturn(customer);
        BDDMockito.when(pharmacyService.findByIdOrThrowNotFound(request.pharmacyId()))
                .thenThrow(new NotFoundException("Pharmacy not found"));

        assertThatThrownBy(() -> service.save(request, actor))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    // ---- update ----

    @Test
    @DisplayName("update should return CompoundingGetResponse when successful")
    void update_ReturnsGetResponse_WhenSuccessful() {
        var compounding = utils.newCompounding();
        var request = utils.newCompoundingPutRequest();
        var customer = customerUtils.newCustomer();
        var pharmacy = pharmacyUtils.newPharmacy();
        var response = utils.newCompoundingGetResponse(compounding);

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));
        BDDMockito.when(customerService.findByIdOrThrowNotFound(request.customerId())).thenReturn(customer);
        BDDMockito.when(pharmacyService.findByIdOrThrowNotFound(request.pharmacyId())).thenReturn(pharmacy);
        BDDMockito.when(repository.save(compounding)).thenReturn(compounding);
        BDDMockito.when(mapper.toCompoundingGetResponse(compounding)).thenReturn(response);

        var result = service.update(compounding.getId(), request);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(compounding);
    }

    @Test
    @DisplayName("update should throw NotFoundException when compounding is not found")
    void update_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newCompoundingPutRequest();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    @Test
    @DisplayName("update should throw ConflictException when compounding is DELIVERED")
    void update_ThrowsConflictException_WhenDelivered() {
        var compounding = utils.newDeliveredCompounding();
        var request = utils.newCompoundingPutRequest();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));

        assertThatThrownBy(() -> service.update(compounding.getId(), request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    // ---- delete ----

    @Test
    @DisplayName("delete should delete compounding when successful")
    void delete_DeletesCompounding_WhenSuccessful() {
        var compounding = utils.newCompounding();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));

        service.delete(compounding.getId());

        BDDMockito.then(repository).should().delete(compounding);
    }

    @Test
    @DisplayName("delete should throw NotFoundException when compounding is not found")
    void delete_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(Compounding.class));
    }

    @Test
    @DisplayName("delete should throw ConflictException when compounding is DELIVERED")
    void delete_ThrowsConflictException_WhenDelivered() {
        var compounding = utils.newDeliveredCompounding();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));

        assertThatThrownBy(() -> service.delete(compounding.getId()))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(Compounding.class));
    }

    // ---- markAsOrdered ----

    @Test
    @DisplayName("markAsOrdered should set ORDERED status when successful")
    void markAsOrdered_SetsOrderedStatus_WhenSuccessful() {
        var compounding = utils.newCompounding();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));
        BDDMockito.when(repository.save(compounding)).thenReturn(compounding);

        service.markAsOrdered(compounding.getId(), actor);

        assertThat(compounding.getStatus()).isEqualTo(CompoundingStatus.ORDERED);
        assertThat(compounding.getOrderedById()).isEqualTo(actor.getId());
        assertThat(compounding.getOrderedByName()).isEqualTo(actor.getName());
        assertThat(compounding.getOrderedAt()).isNotNull();
        BDDMockito.then(repository).should().save(compounding);
    }

    @Test
    @DisplayName("markAsOrdered should throw NotFoundException when compounding is not found")
    void markAsOrdered_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsOrdered(id, actor))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    @Test
    @DisplayName("markAsOrdered should throw ConflictException when compounding is DELIVERED")
    void markAsOrdered_ThrowsConflictException_WhenDelivered() {
        var compounding = utils.newDeliveredCompounding();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));

        assertThatThrownBy(() -> service.markAsOrdered(compounding.getId(), actor))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    // ---- markAsReceived ----

    @Test
    @DisplayName("markAsReceived should set RECEIVED status and fire notification when successful")
    void markAsReceived_SetsReceivedStatusAndFiresNotification_WhenSuccessful() {
        var compounding = utils.newOrderedCompounding();
        var actor = userUtils.newUser();
        var notificationResponse = new NotificationGetResponse(
                UUID.randomUUID(), null, compounding.getId(), CustomerUtils.CUSTOMER_ID,
                "5511999999999", "Test Customer", "Boa tarde, Test Customer!",
                "https://wa.me/5511999999999", Instant.now()
        );

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));
        BDDMockito.when(repository.save(compounding)).thenReturn(compounding);
        BDDMockito.when(notificationService.generateForCompoundingReceived(compounding)).thenReturn(Optional.of(notificationResponse));

        service.markAsReceived(compounding.getId(), actor);

        assertThat(compounding.getStatus()).isEqualTo(CompoundingStatus.RECEIVED);
        assertThat(compounding.getReceivedById()).isEqualTo(actor.getId());
        assertThat(compounding.getReceivedByName()).isEqualTo(actor.getName());
        assertThat(compounding.getReceivedAt()).isNotNull();
        assertThat(compounding.getNotifiedAt()).isNotNull();
        BDDMockito.then(notificationService).should().generateForCompoundingReceived(compounding);
    }

    @Test
    @DisplayName("markAsReceived should not fire notification when already notified")
    void markAsReceived_DoesNotFireNotification_WhenAlreadyNotified() {
        var compounding = utils.newOrderedCompounding();
        compounding.setNotifiedAt(Instant.now());
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));
        BDDMockito.when(repository.save(compounding)).thenReturn(compounding);

        service.markAsReceived(compounding.getId(), actor);

        assertThat(compounding.getStatus()).isEqualTo(CompoundingStatus.RECEIVED);
        BDDMockito.then(notificationService).should(Mockito.never())
                .generateForCompoundingReceived(ArgumentMatchers.any(Compounding.class));
    }

    @Test
    @DisplayName("markAsReceived should throw NotFoundException when compounding is not found")
    void markAsReceived_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsReceived(id, actor))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    @Test
    @DisplayName("markAsReceived should throw ConflictException when compounding is PENDING")
    void markAsReceived_ThrowsConflictException_WhenPending() {
        var compounding = utils.newCompounding();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));

        assertThatThrownBy(() -> service.markAsReceived(compounding.getId(), actor))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    @Test
    @DisplayName("markAsReceived should throw ConflictException when compounding is DELIVERED")
    void markAsReceived_ThrowsConflictException_WhenDelivered() {
        var compounding = utils.newDeliveredCompounding();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));

        assertThatThrownBy(() -> service.markAsReceived(compounding.getId(), actor))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    // ---- markAsDelivered ----

    @Test
    @DisplayName("markAsDelivered should set DELIVERED status when successful")
    void markAsDelivered_SetsDeliveredStatus_WhenSuccessful() {
        var compounding = utils.newReceivedCompounding();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));
        BDDMockito.when(repository.save(compounding)).thenReturn(compounding);

        service.markAsDelivered(compounding.getId(), actor);

        assertThat(compounding.getStatus()).isEqualTo(CompoundingStatus.DELIVERED);
        assertThat(compounding.getDeliveredById()).isEqualTo(actor.getId());
        assertThat(compounding.getDeliveredByName()).isEqualTo(actor.getName());
        assertThat(compounding.getDeliveredAt()).isNotNull();
        BDDMockito.then(repository).should().save(compounding);
    }

    @Test
    @DisplayName("markAsDelivered should throw NotFoundException when compounding is not found")
    void markAsDelivered_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsDelivered(id, actor))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    @Test
    @DisplayName("markAsDelivered should throw ConflictException when compounding is not RECEIVED")
    void markAsDelivered_ThrowsConflictException_WhenNotReceived() {
        var compounding = utils.newCompounding();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));

        assertThatThrownBy(() -> service.markAsDelivered(compounding.getId(), actor))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    // ---- markAsPaid ----

    @Test
    @DisplayName("markAsPaid should set PAID payment status when successful")
    void markAsPaid_SetsPaidStatus_WhenSuccessful() {
        var compounding = utils.newCompounding();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));
        BDDMockito.when(repository.save(compounding)).thenReturn(compounding);

        service.markAsPaid(compounding.getId());

        assertThat(compounding.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        BDDMockito.then(repository).should().save(compounding);
    }

    @Test
    @DisplayName("markAsPaid should throw NotFoundException when compounding is not found")
    void markAsPaid_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsPaid(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    @Test
    @DisplayName("markAsPaid should throw ConflictException when payment is NOTED")
    void markAsPaid_ThrowsConflictException_WhenNoted() {
        var compounding = utils.newCompoundingWithPaymentStatus(PaymentStatus.NOTED);

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));

        assertThatThrownBy(() -> service.markAsPaid(compounding.getId()))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    // ---- markAsMakeNote ----

    @Test
    @DisplayName("markAsMakeNote should set MAKE_NOTE payment status when successful")
    void markAsMakeNote_SetsMakeNoteStatus_WhenSuccessful() {
        var compounding = utils.newCompounding();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));
        BDDMockito.when(repository.save(compounding)).thenReturn(compounding);

        service.markAsMakeNote(compounding.getId());

        assertThat(compounding.getPaymentStatus()).isEqualTo(PaymentStatus.MAKE_NOTE);
        BDDMockito.then(repository).should().save(compounding);
    }

    @Test
    @DisplayName("markAsMakeNote should throw NotFoundException when compounding is not found")
    void markAsMakeNote_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsMakeNote(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    @Test
    @DisplayName("markAsMakeNote should throw ConflictException when payment is PAID")
    void markAsMakeNote_ThrowsConflictException_WhenPaid() {
        var compounding = utils.newCompoundingWithPaymentStatus(PaymentStatus.PAID);

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));

        assertThatThrownBy(() -> service.markAsMakeNote(compounding.getId()))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    @Test
    @DisplayName("markAsMakeNote should throw ConflictException when payment is NOTED")
    void markAsMakeNote_ThrowsConflictException_WhenNoted() {
        var compounding = utils.newCompoundingWithPaymentStatus(PaymentStatus.NOTED);

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));

        assertThatThrownBy(() -> service.markAsMakeNote(compounding.getId()))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    // ---- markAsNoted ----

    @Test
    @DisplayName("markAsNoted should set NOTED payment status when successful from MAKE_NOTE")
    void markAsNoted_SetsNotedStatus_WhenMakeNote() {
        var compounding = utils.newCompoundingWithPaymentStatus(PaymentStatus.MAKE_NOTE);

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));
        BDDMockito.when(repository.save(compounding)).thenReturn(compounding);

        service.markAsNoted(compounding.getId());

        assertThat(compounding.getPaymentStatus()).isEqualTo(PaymentStatus.NOTED);
        BDDMockito.then(repository).should().save(compounding);
    }

    @Test
    @DisplayName("markAsNoted should throw NotFoundException when compounding is not found")
    void markAsNoted_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsNoted(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }

    @Test
    @DisplayName("markAsNoted should throw ConflictException when payment is not MAKE_NOTE")
    void markAsNoted_ThrowsConflictException_WhenNotMakeNote() {
        var compounding = utils.newCompounding();

        BDDMockito.when(repository.findById(compounding.getId())).thenReturn(Optional.of(compounding));

        assertThatThrownBy(() -> service.markAsNoted(compounding.getId()))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Compounding.class));
    }
}
