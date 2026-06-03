package br.com.luizgabriel.farmaorder.stock.order;

import br.com.luizgabriel.farmaorder.commons.CustomerUtils;
import br.com.luizgabriel.farmaorder.commons.OrderUtils;
import br.com.luizgabriel.farmaorder.commons.UserUtils;
import br.com.luizgabriel.farmaorder.customer.Customer;
import br.com.luizgabriel.farmaorder.customer.CustomerService;
import br.com.luizgabriel.farmaorder.exception.ConflictException;
import br.com.luizgabriel.farmaorder.exception.NotFoundException;
import br.com.luizgabriel.farmaorder.notification.NotificationService;
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
class OrderServiceTest {

    @InjectMocks
    private OrderService service;

    @InjectMocks
    private OrderUtils utils;

    @InjectMocks
    private UserUtils userUtils;

    @InjectMocks
    private CustomerUtils customerUtils;

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderMapper mapper;

    @Mock
    private CustomerService customerService;

    @Mock
    private NotificationService notificationService;

    // --- findAll ---

    @Test
    @DisplayName("findAll should return a page of orders when successful")
    void findAll_ReturnsPageOfOrders_WhenSuccessful() {
        var pageable = Pageable.ofSize(10);
        var order = utils.newOrder();
        var response = utils.newOrderGetResponse(order);
        var page = new PageImpl<>(List.of(order));

        BDDMockito.when(repository.findAll(pageable)).thenReturn(page);
        BDDMockito.when(mapper.toOrderGetResponse(order)).thenReturn(response);

        var result = service.findAll(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("findAll should return an empty page when no orders exist")
    void findAll_ReturnsEmptyPage_WhenNoOrdersExist() {
        var pageable = Pageable.ofSize(10);

        BDDMockito.when(repository.findAll(pageable)).thenReturn(Page.empty());

        var result = service.findAll(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // --- findById ---

    @Test
    @DisplayName("findById should return OrderGetResponse when order is found")
    void findById_ReturnsOrderGetResponse_WhenSuccessful() {
        var order = utils.newOrder();
        var response = utils.newOrderGetResponse(order);

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));
        BDDMockito.when(mapper.toOrderGetResponse(order)).thenReturn(response);

        var result = service.findById(order.getId());

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("findById should throw NotFoundException when order is not found")
    void findById_ThrowsNotFoundException_WhenOrderNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    // --- save ---

    @Test
    @DisplayName("save should return OrderPostResponse when successful")
    void save_ReturnsOrderPostResponse_WhenSuccessful() {
        var actor = userUtils.newUser();
        var customer = customerUtils.newCustomer();
        var request = utils.newOrderPostRequest();
        var order = utils.newOrder();
        var item = utils.newPendingItem();
        var response = utils.newOrderPostResponse(order);

        BDDMockito.when(customerService.findByIdOrThrowNotFound(request.customerId())).thenReturn(customer);
        BDDMockito.when(mapper.toOrder(request, customer, actor)).thenReturn(order);
        BDDMockito.when(mapper.toOrderItems(request.items())).thenReturn(List.of(item));
        BDDMockito.when(repository.save(order)).thenReturn(order);
        BDDMockito.when(mapper.toOrderPostResponse(order)).thenReturn(response);

        var result = service.save(request, actor);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(order);
    }

    @Test
    @DisplayName("save should throw NotFoundException when customer is not found")
    void save_ThrowsNotFoundException_WhenCustomerNotFound() {
        var actor = userUtils.newUser();
        var request = utils.newOrderPostRequest();

        BDDMockito.when(customerService.findByIdOrThrowNotFound(request.customerId()))
                .thenThrow(new NotFoundException("Customer not found"));

        assertThatThrownBy(() -> service.save(request, actor))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Order.class));
    }

    // --- update ---

    @Test
    @DisplayName("update should return OrderPutResponse and update customer fields when successful")
    void update_ReturnsOrderPutResponse_WhenSuccessful() {
        var order = utils.newOrder();
        var request = utils.newOrderPutRequest();
        var newCustomer = Customer.builder()
                .id(CustomerUtils.OTHER_CUSTOMER_ID)
                .name("Other Customer")
                .build();
        var response = utils.newOrderPutResponse(order);

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));
        BDDMockito.when(customerService.findByIdOrThrowNotFound(request.customerId())).thenReturn(newCustomer);
        BDDMockito.when(mapper.toOrderPutResponse(order)).thenReturn(response);

        var result = service.update(order.getId(), request);

        assertThat(result).isEqualTo(response);
        assertThat(order.getCustomerId()).isEqualTo(newCustomer.getId());
        assertThat(order.getCustomerName()).isEqualTo(newCustomer.getName());
    }

    @Test
    @DisplayName("update should throw NotFoundException when order is not found")
    void update_ThrowsNotFoundException_WhenOrderNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newOrderPutRequest();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("update should throw ConflictException when order is DELIVERED")
    void update_ThrowsConflictException_WhenOrderIsDelivered() {
        var order = utils.newDeliveredOrder();
        var request = utils.newOrderPutRequest();

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.update(order.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    // --- delete ---

    @Test
    @DisplayName("delete should delete the order when successful")
    void delete_DeletesOrder_WhenSuccessful() {
        var order = utils.newOrder();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        service.delete(order.getId());

        BDDMockito.then(repository).should().delete(order);
    }

    @Test
    @DisplayName("delete should throw NotFoundException when order is not found")
    void delete_ThrowsNotFoundException_WhenOrderNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(Order.class));
    }

    @Test
    @DisplayName("delete should throw ConflictException when order status is DELIVERED")
    void delete_ThrowsConflictException_WhenOrderIsDelivered() {
        var order = utils.newDeliveredOrder();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.delete(order.getId()))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(Order.class));
    }

    @Test
    @DisplayName("delete should throw ConflictException when order has at least one DELIVERED item")
    void delete_ThrowsConflictException_WhenOrderHasDeliveredItems() {
        var order = utils.newOrderWithDeliveredItem();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.delete(order.getId()))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(Order.class));
    }

    // --- addItem ---

    @Test
    @DisplayName("addItem should return OrderItemGetResponse when successful")
    void addItem_ReturnsOrderItemGetResponse_WhenSuccessful() {
        var order = utils.newOrder();
        var request = utils.newOrderItemPostRequest();
        var newItem = utils.newPendingItem();
        var response = utils.newOrderItemGetResponse(newItem);

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));
        BDDMockito.when(mapper.toOrderItem(request)).thenReturn(newItem);
        BDDMockito.when(orderItemRepository.save(newItem)).thenReturn(newItem);
        BDDMockito.when(mapper.toOrderItemGetResponse(newItem)).thenReturn(response);

        var result = service.addItem(order.getId(), request);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(orderItemRepository).should().save(newItem);
    }

    @Test
    @DisplayName("addItem should throw NotFoundException when order is not found")
    void addItem_ThrowsNotFoundException_WhenOrderNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newOrderItemPostRequest();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addItem(id, request))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(orderItemRepository).should(Mockito.never()).save(ArgumentMatchers.any(OrderItem.class));
    }

    @Test
    @DisplayName("addItem should throw ConflictException when order is DELIVERED")
    void addItem_ThrowsConflictException_WhenOrderIsDelivered() {
        var order = utils.newDeliveredOrder();
        var request = utils.newOrderItemPostRequest();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.addItem(order.getId(), request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(orderItemRepository).should(Mockito.never()).save(ArgumentMatchers.any(OrderItem.class));
    }

    // --- updateItem ---

    @Test
    @DisplayName("updateItem should return updated OrderItemGetResponse when successful")
    void updateItem_ReturnsOrderItemGetResponse_WhenSuccessful() {
        var order = utils.newOrder();
        var item = order.getItems().getFirst();
        var request = utils.newOrderItemPutRequest();
        var response = utils.newOrderItemGetResponse(item);

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));
        BDDMockito.when(mapper.toOrderItemGetResponse(item)).thenReturn(response);

        var result = service.updateItem(order.getId(), item.getId(), request);

        assertThat(result).isEqualTo(response);
        assertThat(item.getProduct()).isEqualTo(request.product());
        assertThat(item.getCategory()).isEqualTo(request.category());
        assertThat(item.getQuantity()).isEqualTo(request.quantity());
    }

    @Test
    @DisplayName("updateItem should throw NotFoundException when order is not found")
    void updateItem_ThrowsNotFoundException_WhenOrderNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newOrderItemPutRequest();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateItem(id, UUID.randomUUID(), request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("updateItem should throw NotFoundException when item is not found in the order")
    void updateItem_ThrowsNotFoundException_WhenItemNotFound() {
        var order = utils.newOrder();
        var unknownItemId = UUID.randomUUID();
        var request = utils.newOrderItemPutRequest();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateItem(order.getId(), unknownItemId, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("updateItem should throw ConflictException when item is DELIVERED")
    void updateItem_ThrowsConflictException_WhenItemIsDelivered() {
        var order = utils.newDeliveredOrder();
        var item = order.getItems().getFirst();
        var request = utils.newOrderItemPutRequest();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateItem(order.getId(), item.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    // --- deleteItem ---

    @Test
    @DisplayName("deleteItem should remove the item from the order when successful")
    void deleteItem_RemovesItem_WhenSuccessful() {
        var order = utils.newOrder();
        var item = order.getItems().getFirst();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        service.deleteItem(order.getId(), item.getId());

        assertThat(order.getItems()).doesNotContain(item);
    }

    @Test
    @DisplayName("deleteItem should throw NotFoundException when order is not found")
    void deleteItem_ThrowsNotFoundException_WhenOrderNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteItem(id, UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("deleteItem should throw NotFoundException when item is not found in the order")
    void deleteItem_ThrowsNotFoundException_WhenItemNotFound() {
        var order = utils.newOrder();
        var unknownItemId = UUID.randomUUID();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.deleteItem(order.getId(), unknownItemId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("deleteItem should throw ConflictException when item is DELIVERED")
    void deleteItem_ThrowsConflictException_WhenItemIsDelivered() {
        var order = utils.newDeliveredOrder();
        var item = order.getItems().getFirst();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.deleteItem(order.getId(), item.getId()))
                .isInstanceOf(ConflictException.class);
    }

    // --- markItemAsOrdered ---

    @Test
    @DisplayName("markItemAsOrdered should set ORDERED status and stamp actor fields when successful")
    void markItemAsOrdered_SetsOrderedStatus_WhenSuccessful() {
        var order = utils.newOrder();
        var item = order.getItems().getFirst();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        service.markItemAsOrdered(order.getId(), item.getId(), actor);

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.ORDERED);
        assertThat(item.getOrderedById()).isEqualTo(actor.getId());
        assertThat(item.getOrderedByName()).isEqualTo(actor.getName());
        assertThat(item.getOrderedAt()).isNotNull();
        BDDMockito.then(notificationService).should(Mockito.never())
                .generateForOrderReceived(ArgumentMatchers.any(Order.class));
    }

    @Test
    @DisplayName("markItemAsOrdered should throw NotFoundException when order is not found")
    void markItemAsOrdered_ThrowsNotFoundException_WhenOrderNotFound() {
        var id = UUID.randomUUID();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markItemAsOrdered(id, UUID.randomUUID(), actor))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("markItemAsOrdered should throw NotFoundException when item is not found in the order")
    void markItemAsOrdered_ThrowsNotFoundException_WhenItemNotFound() {
        var order = utils.newOrder();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.markItemAsOrdered(order.getId(), UUID.randomUUID(), actor))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("markItemAsOrdered should throw ConflictException when item is DELIVERED")
    void markItemAsOrdered_ThrowsConflictException_WhenItemIsDelivered() {
        var order = utils.newDeliveredOrder();
        var item = order.getItems().getFirst();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.markItemAsOrdered(order.getId(), item.getId(), actor))
                .isInstanceOf(ConflictException.class);
    }

    // --- markItemAsReceived ---

    @Test
    @DisplayName("markItemAsReceived should set RECEIVED status, stamp actor fields, and trigger notification when order transitions to RECEIVED")
    void markItemAsReceived_SetsReceivedStatusAndTriggersNotification_WhenSuccessful() {
        var order = utils.newOrderedOrder();
        var item = order.getItems().getFirst();
        var actor = userUtils.newUser();
        var notification = utils.newNotificationGetResponse();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));
        BDDMockito.when(notificationService.generateForOrderReceived(order)).thenReturn(notification);

        service.markItemAsReceived(order.getId(), item.getId(), actor);

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.RECEIVED);
        assertThat(item.getReceivedById()).isEqualTo(actor.getId());
        assertThat(item.getReceivedByName()).isEqualTo(actor.getName());
        assertThat(item.getReceivedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(order.getNotifiedAt()).isNotNull();
        BDDMockito.then(notificationService).should().generateForOrderReceived(order);
    }

    @Test
    @DisplayName("markItemAsReceived should throw NotFoundException when order is not found")
    void markItemAsReceived_ThrowsNotFoundException_WhenOrderNotFound() {
        var id = UUID.randomUUID();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markItemAsReceived(id, UUID.randomUUID(), actor))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("markItemAsReceived should throw NotFoundException when item is not found in the order")
    void markItemAsReceived_ThrowsNotFoundException_WhenItemNotFound() {
        var order = utils.newOrderedOrder();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.markItemAsReceived(order.getId(), UUID.randomUUID(), actor))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("markItemAsReceived should throw ConflictException when item is PENDING")
    void markItemAsReceived_ThrowsConflictException_WhenItemIsPending() {
        var order = utils.newOrder();
        var item = order.getItems().getFirst();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.markItemAsReceived(order.getId(), item.getId(), actor))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("markItemAsReceived should throw ConflictException when item is DELIVERED")
    void markItemAsReceived_ThrowsConflictException_WhenItemIsDelivered() {
        var order = utils.newDeliveredOrder();
        var item = order.getItems().getFirst();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.markItemAsReceived(order.getId(), item.getId(), actor))
                .isInstanceOf(ConflictException.class);
    }

    // --- markItemAsDelivered ---

    @Test
    @DisplayName("markItemAsDelivered should set DELIVERED status and stamp actor fields when successful")
    void markItemAsDelivered_SetsDeliveredStatus_WhenSuccessful() {
        var order = utils.newReceivedOrder();
        var item = order.getItems().getFirst();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        service.markItemAsDelivered(order.getId(), item.getId(), actor);

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.DELIVERED);
        assertThat(item.getDeliveredById()).isEqualTo(actor.getId());
        assertThat(item.getDeliveredByName()).isEqualTo(actor.getName());
        assertThat(item.getDeliveredAt()).isNotNull();
    }

    @Test
    @DisplayName("markItemAsDelivered should throw NotFoundException when order is not found")
    void markItemAsDelivered_ThrowsNotFoundException_WhenOrderNotFound() {
        var id = UUID.randomUUID();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markItemAsDelivered(id, UUID.randomUUID(), actor))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("markItemAsDelivered should throw NotFoundException when item is not found in the order")
    void markItemAsDelivered_ThrowsNotFoundException_WhenItemNotFound() {
        var order = utils.newReceivedOrder();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.markItemAsDelivered(order.getId(), UUID.randomUUID(), actor))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("markItemAsDelivered should throw ConflictException when item is not RECEIVED")
    void markItemAsDelivered_ThrowsConflictException_WhenItemIsNotReceived() {
        var order = utils.newOrder();
        var item = order.getItems().getFirst();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.markItemAsDelivered(order.getId(), item.getId(), actor))
                .isInstanceOf(ConflictException.class);
    }

    // --- markAllAsOrdered ---

    @Test
    @DisplayName("markAllAsOrdered should transition all non-DELIVERED items to ORDERED")
    void markAllAsOrdered_TransitionsAllEligibleItems_WhenSuccessful() {
        var order = utils.newOrder();
        var item = order.getItems().getFirst();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        service.markAllAsOrdered(order.getId(), actor);

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.ORDERED);
        BDDMockito.then(notificationService).should(Mockito.never())
                .generateForOrderReceived(ArgumentMatchers.any(Order.class));
    }

    @Test
    @DisplayName("markAllAsOrdered should throw NotFoundException when order is not found")
    void markAllAsOrdered_ThrowsNotFoundException_WhenOrderNotFound() {
        var id = UUID.randomUUID();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAllAsOrdered(id, actor))
                .isInstanceOf(NotFoundException.class);
    }

    // --- markAllAsReceived ---

    @Test
    @DisplayName("markAllAsReceived should transition all ORDERED items to RECEIVED and trigger notification")
    void markAllAsReceived_TransitionsAllEligibleItems_WhenSuccessful() {
        var order = utils.newOrderedOrder();
        var item = order.getItems().getFirst();
        var actor = userUtils.newUser();
        var notification = utils.newNotificationGetResponse();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));
        BDDMockito.when(notificationService.generateForOrderReceived(order)).thenReturn(notification);

        service.markAllAsReceived(order.getId(), actor);

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.RECEIVED);
        assertThat(order.getNotifiedAt()).isNotNull();
        BDDMockito.then(notificationService).should().generateForOrderReceived(order);
    }

    @Test
    @DisplayName("markAllAsReceived should throw NotFoundException when order is not found")
    void markAllAsReceived_ThrowsNotFoundException_WhenOrderNotFound() {
        var id = UUID.randomUUID();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAllAsReceived(id, actor))
                .isInstanceOf(NotFoundException.class);
    }

    // --- markAllAsDelivered ---

    @Test
    @DisplayName("markAllAsDelivered should transition all RECEIVED items to DELIVERED")
    void markAllAsDelivered_TransitionsAllEligibleItems_WhenSuccessful() {
        var order = utils.newReceivedOrder();
        var item = order.getItems().getFirst();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        service.markAllAsDelivered(order.getId(), actor);

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("markAllAsDelivered should throw NotFoundException when order is not found")
    void markAllAsDelivered_ThrowsNotFoundException_WhenOrderNotFound() {
        var id = UUID.randomUUID();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAllAsDelivered(id, actor))
                .isInstanceOf(NotFoundException.class);
    }
}
