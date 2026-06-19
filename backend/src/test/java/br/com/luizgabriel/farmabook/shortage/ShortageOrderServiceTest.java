package br.com.luizgabriel.farmabook.shortage;

import br.com.luizgabriel.farmabook.commons.ShortageOrderUtils;
import br.com.luizgabriel.farmabook.commons.ShortageUtils;
import br.com.luizgabriel.farmabook.commons.UserUtils;
import br.com.luizgabriel.farmabook.exception.ConflictException;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import br.com.luizgabriel.farmabook.distributor.Distributor;
import br.com.luizgabriel.farmabook.distributor.DistributorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ShortageOrderServiceTest {

    @InjectMocks
    private ShortageOrderService service;

    @InjectMocks
    private ShortageOrderUtils utils;

    @InjectMocks
    private ShortageUtils shortageUtils;

    @InjectMocks
    private UserUtils userUtils;

    @Mock
    private ShortageOrderRepository repository;

    @Mock
    private ShortageRepository shortageRepository;

    @Mock
    private DistributorService distributorService;

    @Mock
    private ShortageOrderMapper mapper;

    @Mock
    private ShortageMapper shortageMapper;

    @Test
    @DisplayName("save should create the shortage order and its shortages when successful")
    void save_ReturnsShortageOrderGetResponse_WhenSuccessful() {
        var actor = userUtils.newUser();
        var request = utils.newShortageOrderPostRequest();
        var distributor = utils.newDistributor();
        var order = utils.newShortageOrder();
        var shortage = shortageUtils.newShortage();
        var shortageResponse = shortageUtils.newShortageGetResponse(shortage);
        var response = utils.newShortageOrderGetResponse(order, List.of(shortageResponse));

        BDDMockito.when(distributorService.findByIdOrThrowNotFound(request.distributorId())).thenReturn(distributor);
        BDDMockito.when(mapper.toShortageOrder(request, distributor.getName(), actor)).thenReturn(order);
        BDDMockito.when(repository.save(order)).thenReturn(order);
        BDDMockito.when(shortageRepository.save(ArgumentMatchers.any(Shortage.class))).thenReturn(shortage);
        BDDMockito.when(shortageMapper.toShortageGetResponse(shortage)).thenReturn(shortageResponse);
        BDDMockito.when(mapper.toShortageOrderGetResponse(order, List.of(shortageResponse))).thenReturn(response);

        var result = service.save(request, actor);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(order);
        BDDMockito.then(shortageRepository).should().save(ArgumentMatchers.any(Shortage.class));
    }

    @Test
    @DisplayName("save should throw NotFoundException when distributor is not found")
    void save_ThrowsNotFoundException_WhenDistributorNotFound() {
        var actor = userUtils.newUser();
        var request = utils.newShortageOrderPostRequest();

        BDDMockito.when(distributorService.findByIdOrThrowNotFound(request.distributorId()))
                .thenThrow(new NotFoundException("Distributor not found"));

        assertThatThrownBy(() -> service.save(request, actor))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(ShortageOrder.class));
        BDDMockito.then(shortageRepository).should(Mockito.never()).save(ArgumentMatchers.any(Shortage.class));
    }

    @Test
    @DisplayName("addItem should create a new shortage linked to the order and return the updated order when PENDING")
    void addItem_AddsShortageToOrder_WhenSuccessful() {
        var actor = userUtils.newUser();
        var order = utils.newShortageOrder();
        var request = utils.newShortageOrderItemRequest();
        var shortage = shortageUtils.newShortage();
        var shortageResponse = shortageUtils.newShortageGetResponse(shortage);
        var response = utils.newShortageOrderGetResponse(order, List.of(shortageResponse));

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));
        BDDMockito.when(shortageRepository.save(ArgumentMatchers.any(Shortage.class))).thenReturn(shortage);
        BDDMockito.when(shortageRepository.findAllByShortageOrderId(order.getId())).thenReturn(List.of(shortage));
        BDDMockito.when(shortageMapper.toShortageGetResponse(shortage)).thenReturn(shortageResponse);
        BDDMockito.when(mapper.toShortageOrderGetResponse(order, List.of(shortageResponse))).thenReturn(response);

        var result = service.addItem(order.getId(), request, actor);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(shortageRepository).should().save(ArgumentMatchers.any(Shortage.class));
    }

    @Test
    @DisplayName("addItem should throw NotFoundException when shortage order is not found")
    void addItem_ThrowsNotFoundException_WhenShortageOrderNotFound() {
        var actor = userUtils.newUser();
        var id = UUID.randomUUID();
        var request = utils.newShortageOrderItemRequest();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addItem(id, request, actor))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(shortageRepository).should(Mockito.never()).save(ArgumentMatchers.any(Shortage.class));
    }

    @Test
    @DisplayName("addItem should throw ConflictException when shortage order is already ORDERED")
    void addItem_ThrowsConflictException_WhenShortageOrderIsOrdered() {
        var actor = userUtils.newUser();
        var order = utils.newOrderedShortageOrder();
        var request = utils.newShortageOrderItemRequest();

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.addItem(order.getId(), request, actor))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(shortageRepository).should(Mockito.never()).save(ArgumentMatchers.any(Shortage.class));
    }

    @Test
    @DisplayName("findAll should query by shortage type only when distributorId is null")
    void findAll_QueriesByShortageType_WhenDistributorIdIsNull() {
        var pageable = org.springframework.data.domain.Pageable.ofSize(10);
        var order = utils.newShortageOrder();
        var listResponse = utils.newShortageOrderListResponse(order);
        var page = new org.springframework.data.domain.PageImpl<>(List.of(order));

        BDDMockito.when(repository.findByShortageType(ShortageType.WANIA, pageable)).thenReturn(page);
        BDDMockito.when(mapper.toShortageOrderListResponse(order)).thenReturn(listResponse);

        var result = service.findAll(ShortageType.WANIA, null, pageable);

        assertThat(result.getContent()).containsExactly(listResponse);
        BDDMockito.then(repository).should(Mockito.never())
                .findByShortageTypeAndDistributorId(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    @DisplayName("findAll should query by shortage type and distributor when distributorId is provided")
    void findAll_QueriesByShortageTypeAndDistributor_WhenDistributorIdIsProvided() {
        var pageable = org.springframework.data.domain.Pageable.ofSize(10);
        var distributorId = ShortageOrderUtils.DISTRIBUTOR_ID;
        var order = utils.newShortageOrder();
        var listResponse = utils.newShortageOrderListResponse(order);
        var page = new org.springframework.data.domain.PageImpl<>(List.of(order));

        BDDMockito.when(repository.findByShortageTypeAndDistributorId(ShortageType.WANIA, distributorId, pageable))
                .thenReturn(page);
        BDDMockito.when(mapper.toShortageOrderListResponse(order)).thenReturn(listResponse);

        var result = service.findAll(ShortageType.WANIA, distributorId, pageable);

        assertThat(result.getContent()).containsExactly(listResponse);
        BDDMockito.then(repository).should(Mockito.never())
                .findByShortageType(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    @DisplayName("findById should return ShortageOrderGetResponse with its shortages when successful")
    void findById_ReturnsShortageOrderGetResponse_WhenSuccessful() {
        var order = utils.newShortageOrder();
        var shortage = shortageUtils.newShortage();
        var shortageResponse = shortageUtils.newShortageGetResponse(shortage);
        var response = utils.newShortageOrderGetResponse(order, List.of(shortageResponse));

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));
        BDDMockito.when(shortageRepository.findAllByShortageOrderId(order.getId())).thenReturn(List.of(shortage));
        BDDMockito.when(shortageMapper.toShortageGetResponse(shortage)).thenReturn(shortageResponse);
        BDDMockito.when(mapper.toShortageOrderGetResponse(order, List.of(shortageResponse))).thenReturn(response);

        var result = service.findById(order.getId());

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("findById should throw NotFoundException when shortage order is not found")
    void findById_ThrowsNotFoundException_WhenShortageOrderNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("delete should remove the shortage order and its shortages when PENDING")
    void delete_DeletesShortageOrderAndShortages_WhenSuccessful() {
        var order = utils.newShortageOrder();
        var shortage = shortageUtils.newShortage();

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));
        BDDMockito.when(shortageRepository.findAllByShortageOrderId(order.getId())).thenReturn(List.of(shortage));

        service.delete(order.getId());

        BDDMockito.then(shortageRepository).should().deleteAll(List.of(shortage));
        BDDMockito.then(repository).should().delete(order);
    }

    @Test
    @DisplayName("delete should throw NotFoundException when shortage order is not found")
    void delete_ThrowsNotFoundException_WhenShortageOrderNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(ShortageOrder.class));
    }

    @Test
    @DisplayName("delete should throw ConflictException when shortage order is already ORDERED")
    void delete_ThrowsConflictException_WhenShortageOrderIsOrdered() {
        var order = utils.newOrderedShortageOrder();

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.delete(order.getId()))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(ShortageOrder.class));
        BDDMockito.then(shortageRepository).should(Mockito.never()).delete(ArgumentMatchers.any(Shortage.class));
    }

    @Test
    @DisplayName("markAsOrdered should set ORDERED status on the order and its pending shortages when successful")
    void markAsOrdered_SetsOrderedStatus_WhenSuccessful() {
        var actor = userUtils.newUser();
        var order = utils.newShortageOrder();
        var shortage = shortageUtils.newShortage();

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));
        BDDMockito.when(shortageRepository.findAllByShortageOrderId(order.getId())).thenReturn(List.of(shortage));

        service.markAsOrdered(order.getId(), actor);

        assertThat(order.getStatus()).isEqualTo(ShortageOrderStatus.ORDERED);
        assertThat(order.getOrderedById()).isEqualTo(actor.getId());
        assertThat(order.getOrderedByName()).isEqualTo(actor.getName());
        assertThat(order.getOrderedAt()).isNotNull();
        assertThat(shortage.getStatus()).isEqualTo(ShortageStatus.ORDERED);
        BDDMockito.then(repository).should().save(order);
        BDDMockito.then(shortageRepository).should().save(shortage);
    }

    @Test
    @DisplayName("markAsOrdered should throw NotFoundException when shortage order is not found")
    void markAsOrdered_ThrowsNotFoundException_WhenShortageOrderNotFound() {
        var actor = userUtils.newUser();
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsOrdered(id, actor))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(ShortageOrder.class));
    }

    @Test
    @DisplayName("markAsOrdered should throw ConflictException when shortage order is already ORDERED")
    void markAsOrdered_ThrowsConflictException_WhenShortageOrderIsAlreadyOrdered() {
        var actor = userUtils.newUser();
        var order = utils.newOrderedShortageOrder();

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.markAsOrdered(order.getId(), actor))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(ShortageOrder.class));
        BDDMockito.then(shortageRepository).should(Mockito.never()).save(ArgumentMatchers.any(Shortage.class));
    }

    @Test
    @DisplayName("update should return ShortageOrderGetResponse with updated distributor and observations when successful")
    void update_ReturnsShortageOrderGetResponse_WhenSuccessful() {
        var order = utils.newShortageOrder();
        var request = utils.newShortageOrderPutRequest();
        var newDistributor = Distributor.builder()
                .id(ShortageOrderUtils.DISTRIBUTOR_ID)
                .name("Updated Distributor")
                .build();
        var shortage = shortageUtils.newShortage();
        var shortageResponse = shortageUtils.newShortageGetResponse(shortage);
        var response = utils.newShortageOrderGetResponse(order, List.of(shortageResponse));

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));
        BDDMockito.when(distributorService.findByIdOrThrowNotFound(request.distributorId())).thenReturn(newDistributor);
        BDDMockito.when(shortageRepository.findAllByShortageOrderId(order.getId())).thenReturn(List.of(shortage));
        BDDMockito.when(shortageMapper.toShortageGetResponse(shortage)).thenReturn(shortageResponse);
        BDDMockito.when(mapper.toShortageOrderGetResponse(order, List.of(shortageResponse))).thenReturn(response);

        var result = service.update(order.getId(), request);

        assertThat(result).isEqualTo(response);
        assertThat(order.getDistributorId()).isEqualTo(newDistributor.getId());
        assertThat(order.getDistributorName()).isEqualTo(newDistributor.getName());
        assertThat(order.getObservations()).isEqualTo(request.observations());
        BDDMockito.then(repository).should().save(order);
    }

    @Test
    @DisplayName("update should throw NotFoundException when shortage order is not found")
    void update_ThrowsNotFoundException_WhenShortageOrderNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newShortageOrderPutRequest();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(ShortageOrder.class));
    }

    @Test
    @DisplayName("update should throw ConflictException when shortage order is already ORDERED")
    void update_ThrowsConflictException_WhenShortageOrderIsOrdered() {
        var order = utils.newOrderedShortageOrder();
        var request = utils.newShortageOrderPutRequest();

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.update(order.getId(), request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(ShortageOrder.class));
    }
}
