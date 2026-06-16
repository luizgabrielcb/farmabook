package br.com.luizgabriel.farmabook.stock.order;

import br.com.luizgabriel.farmabook.auth.User;
import br.com.luizgabriel.farmabook.customer.CustomerService;
import br.com.luizgabriel.farmabook.exception.ConflictException;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import br.com.luizgabriel.farmabook.notification.NotificationService;
import br.com.luizgabriel.farmabook.stock.distributor.Distributor;
import br.com.luizgabriel.farmabook.stock.distributor.DistributorService;
import br.com.luizgabriel.farmabook.stock.order.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper mapper;
    private final CustomerService customerService;
    private final NotificationService notificationService;
    private final DistributorService distributorService;

    @Transactional(readOnly = true)
    public Page<OrderGetResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toOrderGetResponse);
    }

    @Transactional(readOnly = true)
    public OrderGetResponse findById(UUID id) {
        var order = findByIdWithItemsOrThrowNotFound(id);

        return mapper.toOrderGetResponse(order);
    }

    @Transactional
    public OrderPostResponse save(OrderPostRequest request, User actor) {
        var customer = customerService.findByIdOrThrowNotFound(request.customerId());

        var order = mapper.toOrder(request, customer, actor);

        var items = mapper.toOrderItems(request.items());

        if (request.paymentStatus() != null) {
            var now = Instant.now();
            items.forEach(item -> {
                item.setPaymentStatus(request.paymentStatus());
                item.setPaymentChangedById(actor.getId());
                item.setPaymentChangedByName(actor.getName());
                item.setPaymentChangedAt(now);
            });
        }

        items.forEach(item -> item.setOrder(order));

        order.setItems(items);

        var saved = repository.save(order);

        return mapper.toOrderPostResponse(saved);
    }

    @Transactional
    public OrderPutResponse update(UUID id, OrderPutRequest request) {
        var order = findByIdWithItemsOrThrowNotFound(id);

        ensureOrderMutable(order);

        var customer = customerService.findByIdOrThrowNotFound(request.customerId());

        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getName());
        order.setObservations(request.observations());
        order.setTotalPrice(request.totalPrice());
        if (request.totalPrice() != null) {
            order.getItems().forEach(item -> item.setPrice(null));
        }

        if (request.paymentStatus() != null) {
            cascadeOrderPaymentStatusToItems(order, request.paymentStatus());
            recalculateOrderPaymentStatus(order);
        }

        return mapper.toOrderPutResponse(order);
    }

    @Transactional
    public void delete(UUID id) {
        var order = findByIdWithItemsOrThrowNotFound(id);

        ensureOrderDeletable(order);

        repository.delete(order);
    }

    @Transactional
    public OrderItemGetResponse addItem(UUID orderId, OrderItemPostRequest request) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);

        ensureOrderMutable(order);

        var item = mapper.toOrderItem(request);
        item.setPrice(request.price());
        item.setOrder(order);

        if (request.price() != null) {
            order.setTotalPrice(null);
        }

        var savedItem = orderItemRepository.save(item);
        order.getItems().add(savedItem);

        recalculateOrderStatus(order);

        return mapper.toOrderItemGetResponse(savedItem);
    }

    @Transactional
    public OrderItemGetResponse updateItem(UUID orderId, UUID itemId, OrderItemPutRequest request) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);

        var item = findItemOrThrowNotFound(order, itemId);

        ensureItemMutable(item);

        item.setProduct(request.product());
        item.setCategory(request.category());
        item.setQuantity(request.quantity());
        item.setPrice(request.price());

        if (request.price() != null) {
            order.setTotalPrice(null);
        }

        return mapper.toOrderItemGetResponse(item);
    }

    @Transactional
    public void deleteItem(UUID orderId, UUID itemId) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);

        var item = findItemOrThrowNotFound(order, itemId);

        ensureItemMutable(item);

        order.getItems().remove(item);
        repository.saveAndFlush(order);

        long remaining = orderItemRepository.countActiveByOrderId(orderId);
        if (remaining == 0) {
            order.setStatus(OrderStatus.PENDING);
            recalculateOrderPaymentStatus(order);
            repository.save(order);
            return;
        }

        recalculateOrderStatus(order);
    }

    @Transactional
    public void markItemAsOrdered(UUID orderId, UUID itemId, User actor, UUID distributorId) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);

        var item = findItemOrThrowNotFound(order, itemId);

        var distributor = distributorService.findByIdOrThrowNotFound(distributorId);

        applyMarkAsOrdered(item, actor, distributor);

        recalculateOrderStatus(order);
    }

    @Transactional
    public void markItemAsReceived(UUID orderId, UUID itemId, User actor) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);

        var item = findItemOrThrowNotFound(order, itemId);

        applyMarkAsReceived(item, actor);

        recalculateOrderStatus(order);
    }

    @Transactional
    public void markItemAsDelivered(UUID orderId, UUID itemId, User actor) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);

        var item = findItemOrThrowNotFound(order, itemId);

        applyMarkAsDelivered(item, actor);

        recalculateOrderStatus(order);
    }

    @Transactional
    public void markAllAsOrdered(UUID orderId, User actor, UUID distributorId) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);

        var distributor = distributorService.findByIdOrThrowNotFound(distributorId);

        order.getItems().stream()
                .filter(i -> i.getStatus() != OrderItemStatus.DELIVERED)
                .forEach(i -> applyMarkAsOrdered(i, actor, distributor));

        recalculateOrderStatus(order);
    }

    @Transactional
    public void markAllAsReceived(UUID orderId, User actor) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);

        order.getItems().stream()
                .filter(i -> i.getStatus() == OrderItemStatus.ORDERED
                        || i.getStatus() == OrderItemStatus.RECEIVED)
                .forEach(i -> applyMarkAsReceived(i, actor));

        recalculateOrderStatus(order);
    }

    @Transactional
    public void markAllAsDelivered(UUID orderId, User actor) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);

        order.getItems().stream()
                .filter(i -> i.getStatus() == OrderItemStatus.RECEIVED)
                .forEach(i -> applyMarkAsDelivered(i, actor));

        recalculateOrderStatus(order);
    }

    @Transactional
    public void markItemPaymentAsPaid(UUID orderId, UUID itemId, User actor) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);
        var item = findItemOrThrowNotFound(order, itemId);
        if (item.getPaymentStatus() == OrderPaymentStatus.NOTED) {
            throw new ConflictException("Item with id '" + itemId + "' payment is NOTED and cannot be changed");
        }
        item.setPaymentStatus(OrderPaymentStatus.PAID);
        stampPaymentChange(item, actor);
        recalculateOrderPaymentStatus(order);
        repository.save(order);
    }

    @Transactional
    public void markItemPaymentAsMakeNote(UUID orderId, UUID itemId, User actor) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);
        var item = findItemOrThrowNotFound(order, itemId);
        if (item.getPaymentStatus() == OrderPaymentStatus.PAID) {
            throw new ConflictException("Item with id '" + itemId + "' payment is PAID and cannot be changed");
        }
        if (item.getPaymentStatus() == OrderPaymentStatus.NOTED) {
            throw new ConflictException("Item with id '" + itemId + "' payment is NOTED and cannot be changed");
        }
        item.setPaymentStatus(OrderPaymentStatus.MAKE_NOTE);
        stampPaymentChange(item, actor);
        recalculateOrderPaymentStatus(order);
        repository.save(order);
    }

    @Transactional
    public void markItemPaymentAsNoted(UUID orderId, UUID itemId, User actor) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);
        var item = findItemOrThrowNotFound(order, itemId);
        if (item.getPaymentStatus() != OrderPaymentStatus.MAKE_NOTE) {
            throw new ConflictException("Item with id '" + itemId + "' payment must be MAKE_NOTE to transition to NOTED");
        }
        item.setPaymentStatus(OrderPaymentStatus.NOTED);
        stampPaymentChange(item, actor);
        recalculateOrderPaymentStatus(order);
        repository.save(order);
    }

    @Transactional
    public void markItemPaymentAsToPay(UUID orderId, UUID itemId, User actor) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);
        var item = findItemOrThrowNotFound(order, itemId);
        if (item.getPaymentStatus() != OrderPaymentStatus.MAKE_NOTE) {
            throw new ConflictException("Item with id '" + itemId + "' payment must be MAKE_NOTE to revert to TO_PAY");
        }
        item.setPaymentStatus(OrderPaymentStatus.TO_PAY);
        stampPaymentChange(item, actor);
        recalculateOrderPaymentStatus(order);
        repository.save(order);
    }

    private void stampPaymentChange(OrderItem item, User actor) {
        item.setPaymentChangedById(actor.getId());
        item.setPaymentChangedByName(actor.getName());
        item.setPaymentChangedAt(Instant.now());
    }

    private void applyMarkAsOrdered(OrderItem item, User actor, Distributor distributor) {
        if (item.getStatus() == OrderItemStatus.DELIVERED) {
            throw new ConflictException(
                    "Item with id '" + item.getId() + "' is DELIVERED and cannot be marked as ORDERED");
        }
        if (item.getStatus() == OrderItemStatus.ORDERED) {
            return;
        }

        item.setReceivedById(null);
        item.setReceivedByName(null);
        item.setReceivedAt(null);

        item.setStatus(OrderItemStatus.ORDERED);
        item.setOrderedById(actor.getId());
        item.setOrderedByName(actor.getName());
        item.setOrderedAt(Instant.now());
        item.setDistributorId(distributor.getId());
        item.setDistributorName(distributor.getName());
    }

    private void applyMarkAsReceived(OrderItem item, User actor) {
        if (item.getStatus() == OrderItemStatus.PENDING) {
            throw new ConflictException(
                    "Item with id '" + item.getId() + "' cannot be marked as RECEIVED from PENDING");
        }
        if (item.getStatus() == OrderItemStatus.DELIVERED) {
            throw new ConflictException(
                    "Item with id '" + item.getId() + "' is DELIVERED and cannot be marked as RECEIVED");
        }
        if (item.getStatus() == OrderItemStatus.RECEIVED) {
            return;
        }

        item.setStatus(OrderItemStatus.RECEIVED);
        item.setReceivedById(actor.getId());
        item.setReceivedByName(actor.getName());
        item.setReceivedAt(Instant.now());
    }

    private void applyMarkAsDelivered(OrderItem item, User actor) {
        if (item.getStatus() != OrderItemStatus.RECEIVED) {
            throw new ConflictException(
                    "Item with id '" + item.getId() + "' cannot be marked as DELIVERED from status " + item.getStatus());
        }

        item.setStatus(OrderItemStatus.DELIVERED);
        item.setDeliveredById(actor.getId());
        item.setDeliveredByName(actor.getName());
        item.setDeliveredAt(Instant.now());
    }

    private void cascadeOrderPaymentStatusToItems(Order order, OrderPaymentStatus target) {
        for (var item : order.getItems()) {
            boolean eligible = switch (target) {
                case PAID -> item.getPaymentStatus() == OrderPaymentStatus.TO_PAY || item.getPaymentStatus() == OrderPaymentStatus.MAKE_NOTE;
                case MAKE_NOTE -> item.getPaymentStatus() == OrderPaymentStatus.TO_PAY;
                case NOTED -> item.getPaymentStatus() == OrderPaymentStatus.MAKE_NOTE;
                case TO_PAY -> item.getPaymentStatus() == OrderPaymentStatus.MAKE_NOTE;
            };
            if (eligible) item.setPaymentStatus(target);
        }
    }

    private void recalculateOrderPaymentStatus(Order order) {
        var minPaymentStatus = order.getItems().stream()
                .map(OrderItem::getPaymentStatus)
                .min(Comparator.naturalOrder())
                .orElse(OrderPaymentStatus.TO_PAY);
        order.setPaymentStatus(minPaymentStatus);
    }

    private void recalculateOrderStatus(Order order) {
        var previousStatus = order.getStatus();

        var minItemStatus = order.getItems().stream()
                .map(OrderItem::getStatus)
                .min(Comparator.naturalOrder())
                .orElse(OrderItemStatus.PENDING);

        var newStatus = OrderStatus.valueOf(minItemStatus.name());

        order.setStatus(newStatus);

        boolean transitionedToReceived =
                previousStatus.ordinal() < OrderStatus.RECEIVED.ordinal()
                        && newStatus == OrderStatus.RECEIVED;


        if (transitionedToReceived) {
            notificationService.generateForOrderReceived(order)
                    .ifPresent(notification -> order.setNotifiedAt(notification.sentAt()));
        }

        recalculateOrderPaymentStatus(order);
        repository.save(order);
    }

    private Order findByIdOrThrowNotFound(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order with id '" + id + "' not found"));
    }

    private void ensureOrderDeletable(Order order) {
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new ConflictException(
                    "Order with id '" + order.getId() + "' is DELIVERED and cannot be deleted");
        }

        boolean anyDelivered = order.getItems().stream()
                .anyMatch(i -> i.getStatus() == OrderItemStatus.DELIVERED);

        if (anyDelivered) {
            throw new ConflictException(
                    "Order with id '" + order.getId() + "' has DELIVERED items and cannot be deleted");
        }
    }

    private Order findByIdWithItemsOrThrowNotFound(UUID id) {
        return repository.findWithItemsById(id)
                .orElseThrow(() -> new NotFoundException("Order with id '" + id + "' not found"));
    }

    private OrderItem findItemOrThrowNotFound(Order order, UUID itemId) {
        return order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Item with id '" + itemId + "' not found in order '" + order.getId() + "'"));
    }

    private void ensureOrderMutable(Order order) {
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new ConflictException(
                    "Order with id '" + order.getId() + "' is DELIVERED and cannot be modified");
        }
    }

    private void ensureItemMutable(OrderItem item) {
        if (item.getStatus() == OrderItemStatus.DELIVERED) {
            throw new ConflictException(
                    "Item with id '" + item.getId() + "' is DELIVERED and cannot be modified");
        }
    }
}
