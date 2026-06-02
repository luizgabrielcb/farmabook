package br.com.luizgabriel.farmaorder.stock.order;

import br.com.luizgabriel.farmaorder.auth.User;
import br.com.luizgabriel.farmaorder.customer.CustomerService;
import br.com.luizgabriel.farmaorder.exception.ConflictException;
import br.com.luizgabriel.farmaorder.exception.NotFoundException;
import br.com.luizgabriel.farmaorder.stock.order.dto.*;
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

        items.forEach(item -> item.setOrder(order));

        order.setItems(items);

        var saved = repository.save(order);

        return mapper.toOrderPostResponse(saved);
    }

    @Transactional
    public OrderPutResponse update(UUID id, OrderPutRequest request) {
        var order = findByIdOrThrowNotFound(id);

        ensureOrderMutable(order);

        var customer = customerService.findByIdOrThrowNotFound(request.customerId());

        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getName());

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
        item.setOrder(order);

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

        return mapper.toOrderItemGetResponse(item);
    }

    @Transactional
    public void deleteItem(UUID orderId, UUID itemId) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);

        var item = findItemOrThrowNotFound(order, itemId);

        ensureItemMutable(item);

        order.getItems().remove(item);

        recalculateOrderStatus(order);
    }

    @Transactional
    public void markItemAsOrdered(UUID orderId, UUID itemId, User actor) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);

        var item = findItemOrThrowNotFound(order, itemId);

        applyMarkAsOrdered(item, actor);

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
    public void markAllAsOrdered(UUID orderId, User actor) {
        var order = findByIdWithItemsOrThrowNotFound(orderId);

        order.getItems().stream()
                .filter(i -> i.getStatus() != OrderItemStatus.DELIVERED)
                .forEach(i -> applyMarkAsOrdered(i, actor));

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

    private void applyMarkAsOrdered(OrderItem item, User actor) {
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
            var customer = customerService.findByIdOrThrowNotFound(order.getCustomerId());

            order.setNotifiedAt(Instant.now());
        }
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
