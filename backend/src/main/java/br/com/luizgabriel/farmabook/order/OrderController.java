package br.com.luizgabriel.farmabook.order;

import br.com.luizgabriel.farmabook.auth.AuthService;
import br.com.luizgabriel.farmabook.order.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<OrderPostResponse> save(
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid OrderPostRequest request) {

        var actor = authService.authenticatedUser(pin);

        var response = service.save(request, actor);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderGetResponse>> findAll(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        var orderGetResponsePage = service.findAll(pageable);

        return ResponseEntity.ok(orderGetResponsePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderGetResponse> findById(@PathVariable UUID id) {
        var orderGetResponse = service.findById(id);

        return ResponseEntity.ok(orderGetResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderPutResponse> update(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid OrderPutRequest request) {

        authService.authenticatedUser(pin);

        var response = service.update(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestHeader("X-Auth-Pin") String pin) {

        authService.authenticatedUser(pin);

        service.delete(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/mark-as-ordered")
    public ResponseEntity<Void> markAllAsOrdered(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid MarkItemAsOrderedRequest request) {

        var actor = authService.authenticatedUser(pin);

        service.markAllAsOrdered(id, actor, request.distributorId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/mark-as-received")
    public ResponseEntity<Void> markAllAsReceived(@PathVariable UUID id, @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markAllAsReceived(id, actor);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/mark-as-delivered")
    public ResponseEntity<Void> markAllAsDelivered(@PathVariable UUID id, @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markAllAsDelivered(id, actor);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderItemGetResponse> addItem(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid OrderItemPostRequest request) {

        var actor = authService.authenticatedUser(pin);

        var response = service.addItem(id, request, actor);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<OrderItemGetResponse> updateItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid OrderItemPutRequest request) {

        authService.authenticatedUser(pin);

        var response = service.updateItem(id, itemId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestHeader("X-Auth-Pin") String pin) {

        authService.authenticatedUser(pin);

        service.deleteItem(id, itemId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/items/{itemId}/mark-as-ordered")
    public ResponseEntity<Void> markItemAsOrdered(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid MarkItemAsOrderedRequest request) {

        var actor = authService.authenticatedUser(pin);

        service.markItemAsOrdered(id, itemId, actor, request.distributorId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/items/{itemId}/mark-as-received")
    public ResponseEntity<Void> markItemAsReceived(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markItemAsReceived(id, itemId, actor);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/items/{itemId}/mark-as-delivered")
    public ResponseEntity<Void> markItemAsDelivered(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markItemAsDelivered(id, itemId, actor);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/items/{itemId}/payment/mark-as-paid")
    public ResponseEntity<Void> markItemPaymentAsPaid(
            @PathVariable UUID id, @PathVariable UUID itemId,
            @RequestHeader("X-Auth-Pin") String pin) {
        var actor = authService.authenticatedUser(pin);
        service.markItemPaymentAsPaid(id, itemId, actor);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/items/{itemId}/payment/mark-as-make-note")
    public ResponseEntity<Void> markItemPaymentAsMakeNote(
            @PathVariable UUID id, @PathVariable UUID itemId,
            @RequestHeader("X-Auth-Pin") String pin) {
        var actor = authService.authenticatedUser(pin);
        service.markItemPaymentAsMakeNote(id, itemId, actor);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/items/{itemId}/payment/mark-as-noted")
    public ResponseEntity<Void> markItemPaymentAsNoted(
            @PathVariable UUID id, @PathVariable UUID itemId,
            @RequestHeader("X-Auth-Pin") String pin) {
        var actor = authService.authenticatedUser(pin);
        service.markItemPaymentAsNoted(id, itemId, actor);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/items/{itemId}/payment/mark-as-to-pay")
    public ResponseEntity<Void> markItemPaymentAsToPay(
            @PathVariable UUID id, @PathVariable UUID itemId,
            @RequestHeader("X-Auth-Pin") String pin) {
        var actor = authService.authenticatedUser(pin);
        service.markItemPaymentAsToPay(id, itemId, actor);
        return ResponseEntity.noContent().build();
    }
}
