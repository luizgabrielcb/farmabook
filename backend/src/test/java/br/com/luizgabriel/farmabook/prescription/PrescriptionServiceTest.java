package br.com.luizgabriel.farmabook.prescription;

import br.com.luizgabriel.farmabook.commons.CustomerUtils;
import br.com.luizgabriel.farmabook.commons.PrescriptionUtils;
import br.com.luizgabriel.farmabook.commons.UserUtils;
import br.com.luizgabriel.farmabook.customer.CustomerService;
import br.com.luizgabriel.farmabook.exception.ConflictException;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import br.com.luizgabriel.farmabook.prescription.dto.PrescriptionItemPostRequest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @InjectMocks
    private PrescriptionService service;

    @InjectMocks
    private PrescriptionUtils utils;

    @InjectMocks
    private UserUtils userUtils;

    @InjectMocks
    private CustomerUtils customerUtils;

    @Mock
    private PrescriptionRepository repository;

    @Mock
    private PrescriptionItemRepository itemRepository;

    @Mock
    private PrescriptionMapper mapper;

    @Mock
    private CustomerService customerService;

    @Test
    @DisplayName("findAll should return a page of prescriptions when successful")
    void findAll_ReturnsPageOfPrescriptions_WhenSuccessful() {
        var pageable = Pageable.ofSize(10);
        var prescription = utils.newPrescription();
        var response = utils.newPrescriptionGetResponse(prescription);
        var page = new PageImpl<>(List.of(prescription));

        BDDMockito.when(repository.findAll(pageable)).thenReturn(page);
        BDDMockito.when(mapper.toPrescriptionGetResponse(prescription)).thenReturn(response);

        var result = service.findAll(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("findAll should return an empty page when no prescriptions exist")
    void findAll_ReturnsEmptyPage_WhenNoPrescriptionsExist() {
        var pageable = Pageable.ofSize(10);

        BDDMockito.when(repository.findAll(pageable)).thenReturn(Page.empty());

        var result = service.findAll(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findById should return PrescriptionGetResponse when prescription is found")
    void findById_ReturnsPrescriptionGetResponse_WhenSuccessful() {
        var prescription = utils.newPrescription();
        var response = utils.newPrescriptionGetResponse(prescription);

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));
        BDDMockito.when(mapper.toPrescriptionGetResponse(prescription)).thenReturn(response);

        var result = service.findById(prescription.getId());

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("findById should throw NotFoundException when prescription is not found")
    void findById_ThrowsNotFoundException_WhenPrescriptionNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("save should return PrescriptionPostResponse when successful")
    void save_ReturnsPrescriptionPostResponse_WhenSuccessful() {
        var actor = userUtils.newUser();
        var customer = customerUtils.newCustomer();
        var request = utils.newPrescriptionPostRequest();
        var prescription = utils.newPrescription();
        var response = utils.newPrescriptionPostResponse(prescription);

        BDDMockito.when(customerService.findByIdOrThrowNotFound(request.customerId())).thenReturn(customer);
        BDDMockito.when(mapper.toPrescription(request, customer, actor)).thenReturn(prescription);
        BDDMockito.when(mapper.toPrescriptionItems(request.items())).thenReturn(prescription.getItems());
        BDDMockito.when(repository.save(prescription)).thenReturn(prescription);
        BDDMockito.when(mapper.toPrescriptionPostResponse(prescription)).thenReturn(response);

        var result = service.save(request, actor);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(prescription);
    }

    @Test
    @DisplayName("save should throw NotFoundException when customer is not found")
    void save_ThrowsNotFoundException_WhenCustomerNotFound() {
        var actor = userUtils.newUser();
        var request = utils.newPrescriptionPostRequest();

        BDDMockito.when(customerService.findByIdOrThrowNotFound(request.customerId()))
                .thenThrow(new NotFoundException("Customer not found"));

        assertThatThrownBy(() -> service.save(request, actor))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Prescription.class));
    }

    @Test
    @DisplayName("update should update customer and observations and return PrescriptionPutResponse when successful")
    void update_ReturnsResponse_WhenSuccessful() {
        var prescription = utils.newPrescription();
        var request = utils.newPrescriptionPutRequest();
        var newCustomer = customerUtils.newOtherCustomer();
        var response = utils.newPrescriptionPutResponse(prescription);

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));
        BDDMockito.when(customerService.findByIdOrThrowNotFound(request.customerId())).thenReturn(newCustomer);
        BDDMockito.when(mapper.toPrescriptionPutResponse(prescription)).thenReturn(response);

        var result = service.update(prescription.getId(), request);

        assertThat(result).isEqualTo(response);
        assertThat(prescription.getCustomerId()).isEqualTo(newCustomer.getId());
        assertThat(prescription.getCustomerName()).isEqualTo(newCustomer.getName());
        assertThat(prescription.getObservations()).isEqualTo(request.observations());
    }

    @Test
    @DisplayName("update should throw NotFoundException when prescription is not found")
    void update_ThrowsNotFoundException_WhenPrescriptionNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newPrescriptionPutRequest();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("update should throw ConflictException when prescription is FINISHED")
    void update_ThrowsConflictException_WhenPrescriptionIsFinished() {
        var prescription = utils.newFinishedPrescription();
        var request = utils.newPrescriptionPutRequest();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.update(prescription.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("delete should delete the prescription when successful")
    void delete_DeletesPrescription_WhenSuccessful() {
        var prescription = utils.newPrescription();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        service.delete(prescription.getId());

        BDDMockito.then(repository).should().delete(prescription);
    }

    @Test
    @DisplayName("delete should throw NotFoundException when prescription is not found")
    void delete_ThrowsNotFoundException_WhenPrescriptionNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(Prescription.class));
    }

    @Test
    @DisplayName("delete should throw ConflictException when prescription is FINISHED")
    void delete_ThrowsConflictException_WhenPrescriptionIsFinished() {
        var prescription = utils.newFinishedPrescription();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.delete(prescription.getId()))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(Prescription.class));
    }

    @Test
    @DisplayName("addItem should save and return the new item when successful")
    void addItem_ReturnsItemGetResponse_WhenSuccessful() {
        var prescription = utils.newPrescription();
        var request = utils.newPrescriptionItemPostRequest();
        var newItem = utils.newPendingItem();
        var response = utils.newPrescriptionItemGetResponse(newItem);

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));
        BDDMockito.when(mapper.toPrescriptionItem(request)).thenReturn(newItem);
        BDDMockito.when(itemRepository.save(newItem)).thenReturn(newItem);
        BDDMockito.when(mapper.toPrescriptionItemGetResponse(newItem)).thenReturn(response);

        var result = service.addItem(prescription.getId(), request);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(itemRepository).should().save(newItem);
    }

    @Test
    @DisplayName("addItem should throw NotFoundException when prescription is not found")
    void addItem_ThrowsNotFoundException_WhenPrescriptionNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newPrescriptionItemPostRequest();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addItem(id, request))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(itemRepository).should(Mockito.never()).save(ArgumentMatchers.any(PrescriptionItem.class));
    }

    @Test
    @DisplayName("addItem should throw ConflictException when prescription is FINISHED")
    void addItem_ThrowsConflictException_WhenPrescriptionIsFinished() {
        var prescription = utils.newFinishedPrescription();
        var request = utils.newPrescriptionItemPostRequest();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.addItem(prescription.getId(), request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(itemRepository).should(Mockito.never()).save(ArgumentMatchers.any(PrescriptionItem.class));
    }

    @Test
    @DisplayName("updateItem should update item fields and return response when successful")
    void updateItem_ReturnsItemGetResponse_WhenSuccessful() {
        var prescription = utils.newPrescription();
        var item = prescription.getItems().getFirst();
        var request = utils.newPrescriptionItemPutRequest();
        var response = utils.newPrescriptionItemGetResponse(item);

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));
        BDDMockito.when(mapper.toPrescriptionItemGetResponse(item)).thenReturn(response);

        var result = service.updateItem(prescription.getId(), item.getId(), request);

        assertThat(result).isEqualTo(response);
        assertThat(item.getProduct()).isEqualTo(request.product());
        assertThat(item.getQuantity()).isEqualTo(request.quantity());
        assertThat(item.getBatch()).isEqualTo(request.batch());
        assertThat(item.getExpiry()).isEqualTo(request.expiry());
    }

    @Test
    @DisplayName("updateItem should throw NotFoundException when prescription is not found")
    void updateItem_ThrowsNotFoundException_WhenPrescriptionNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newPrescriptionItemPutRequest();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateItem(id, UUID.randomUUID(), request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("updateItem should throw NotFoundException when item is not found in the prescription")
    void updateItem_ThrowsNotFoundException_WhenItemNotFound() {
        var prescription = utils.newPrescription();
        var request = utils.newPrescriptionItemPutRequest();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.updateItem(prescription.getId(), UUID.randomUUID(), request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("updateItem should throw ConflictException when prescription is FINISHED")
    void updateItem_ThrowsConflictException_WhenPrescriptionIsFinished() {
        var prescription = utils.newFinishedPrescription();
        var item = prescription.getItems().getFirst();
        var request = utils.newPrescriptionItemPutRequest();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.updateItem(prescription.getId(), item.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("updateItem should throw ConflictException when item is RECEIVED")
    void updateItem_ThrowsConflictException_WhenItemIsReceived() {
        var prescription = utils.newPrescriptionWithReceivedItem();
        var receivedItem = prescription.getItems().stream()
                .filter(i -> i.getStatus() == PrescriptionItemStatus.RECEIVED)
                .findFirst().orElseThrow();
        var request = utils.newPrescriptionItemPutRequest();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.updateItem(prescription.getId(), receivedItem.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("deleteItem should remove the item from the prescription when successful")
    void deleteItem_RemovesItem_WhenSuccessful() {
        var prescription = utils.newPrescription();
        var item = prescription.getItems().getFirst();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));
        BDDMockito.when(repository.saveAndFlush(prescription)).thenReturn(prescription);

        service.deleteItem(prescription.getId(), item.getId());

        assertThat(prescription.getItems()).doesNotContain(item);
    }

    @Test
    @DisplayName("deleteItem should throw NotFoundException when prescription is not found")
    void deleteItem_ThrowsNotFoundException_WhenPrescriptionNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteItem(id, UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("deleteItem should throw NotFoundException when item is not found in the prescription")
    void deleteItem_ThrowsNotFoundException_WhenItemNotFound() {
        var prescription = utils.newPrescription();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.deleteItem(prescription.getId(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("deleteItem should throw ConflictException when prescription is FINISHED")
    void deleteItem_ThrowsConflictException_WhenPrescriptionIsFinished() {
        var prescription = utils.newFinishedPrescription();
        var item = prescription.getItems().getFirst();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.deleteItem(prescription.getId(), item.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("deleteItem should throw ConflictException when item is RECEIVED")
    void deleteItem_ThrowsConflictException_WhenItemIsReceived() {
        var prescription = utils.newPrescriptionWithReceivedItem();
        var receivedItem = prescription.getItems().stream()
                .filter(i -> i.getStatus() == PrescriptionItemStatus.RECEIVED)
                .findFirst().orElseThrow();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.deleteItem(prescription.getId(), receivedItem.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("markItemAsReceived should set RECEIVED and transition prescription to FINISHED when it is the last pending item")
    void markItemAsReceived_SetsReceivedAndFinishesPrescription_WhenLastPendingItem() {
        var prescription = utils.newPrescription();
        var item = prescription.getItems().getFirst();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));
        BDDMockito.when(repository.save(prescription)).thenReturn(prescription);

        service.markItemAsReceived(prescription.getId(), item.getId(), actor);

        assertThat(item.getStatus()).isEqualTo(PrescriptionItemStatus.RECEIVED);
        assertThat(item.getReceivedById()).isEqualTo(actor.getId());
        assertThat(item.getReceivedByName()).isEqualTo(actor.getName());
        assertThat(item.getReceivedAt()).isNotNull();
        assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.FINISHED);
        BDDMockito.then(repository).should().save(prescription);
    }

    @Test
    @DisplayName("markItemAsReceived should keep prescription PENDING when other items remain pending")
    void markItemAsReceived_KeepsPrescriptionPending_WhenOtherItemsRemainPending() {
        var prescription = utils.newPrescriptionWithReceivedItem();
        var pendingItem = prescription.getItems().stream()
                .filter(i -> i.getStatus() == PrescriptionItemStatus.PENDING)
                .findFirst().orElseThrow();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));
        BDDMockito.when(repository.save(prescription)).thenReturn(prescription);

        service.markItemAsReceived(prescription.getId(), pendingItem.getId(), actor);

        assertThat(pendingItem.getStatus()).isEqualTo(PrescriptionItemStatus.RECEIVED);
        assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.FINISHED);
    }

    @Test
    @DisplayName("markItemAsReceived should throw NotFoundException when prescription is not found")
    void markItemAsReceived_ThrowsNotFoundException_WhenPrescriptionNotFound() {
        var id = UUID.randomUUID();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markItemAsReceived(id, UUID.randomUUID(), actor))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("markItemAsReceived should throw NotFoundException when item is not found in the prescription")
    void markItemAsReceived_ThrowsNotFoundException_WhenItemNotFound() {
        var prescription = utils.newPrescription();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.markItemAsReceived(prescription.getId(), UUID.randomUUID(), actor))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("markItemAsReceived should throw ConflictException when prescription is FINISHED")
    void markItemAsReceived_ThrowsConflictException_WhenPrescriptionIsFinished() {
        var prescription = utils.newFinishedPrescription();
        var item = prescription.getItems().getFirst();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.markItemAsReceived(prescription.getId(), item.getId(), actor))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Prescription.class));
    }

    @Test
    @DisplayName("markItemAsReceived should throw ConflictException when item is already RECEIVED")
    void markItemAsReceived_ThrowsConflictException_WhenItemAlreadyReceived() {
        var prescription = utils.newPrescriptionWithReceivedItem();
        var receivedItem = prescription.getItems().stream()
                .filter(i -> i.getStatus() == PrescriptionItemStatus.RECEIVED)
                .findFirst().orElseThrow();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.markItemAsReceived(prescription.getId(), receivedItem.getId(), actor))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Prescription.class));
    }

    @Test
    @DisplayName("markAllAsReceived should transition all PENDING items to RECEIVED and set prescription to FINISHED")
    void markAllAsReceived_TransitionsAllItemsAndFinishesPrescription_WhenSuccessful() {
        var prescription = utils.newPrescription();
        var item = prescription.getItems().getFirst();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));
        BDDMockito.when(repository.save(prescription)).thenReturn(prescription);

        service.markAllAsReceived(prescription.getId(), actor);

        assertThat(item.getStatus()).isEqualTo(PrescriptionItemStatus.RECEIVED);
        assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.FINISHED);
        BDDMockito.then(repository).should().save(prescription);
    }

    @Test
    @DisplayName("markAllAsReceived should skip already RECEIVED items and not fail")
    void markAllAsReceived_SkipsAlreadyReceivedItems_WhenTolerant() {
        var prescription = utils.newPrescriptionWithReceivedItem();
        var pendingItem = prescription.getItems().stream()
                .filter(i -> i.getStatus() == PrescriptionItemStatus.PENDING)
                .findFirst().orElseThrow();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));
        BDDMockito.when(repository.save(prescription)).thenReturn(prescription);

        service.markAllAsReceived(prescription.getId(), actor);

        assertThat(pendingItem.getStatus()).isEqualTo(PrescriptionItemStatus.RECEIVED);
        assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.FINISHED);
    }

    @Test
    @DisplayName("markAllAsReceived should throw NotFoundException when prescription is not found")
    void markAllAsReceived_ThrowsNotFoundException_WhenPrescriptionNotFound() {
        var id = UUID.randomUUID();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAllAsReceived(id, actor))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("markAllAsReceived should throw ConflictException when prescription is FINISHED")
    void markAllAsReceived_ThrowsConflictException_WhenPrescriptionIsFinished() {
        var prescription = utils.newFinishedPrescription();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.markAllAsReceived(prescription.getId(), actor))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Prescription.class));
    }
}
