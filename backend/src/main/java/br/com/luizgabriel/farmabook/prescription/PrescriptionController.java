package br.com.luizgabriel.farmabook.prescription;

import br.com.luizgabriel.farmabook.auth.AuthService;
import br.com.luizgabriel.farmabook.prescription.dto.*;
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
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService service;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<PrescriptionPostResponse> save(
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid PrescriptionPostRequest request) {

        var actor = authService.authenticatedUser(pin);

        var response = service.save(request, actor);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<PrescriptionGetResponse>> findAll(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        var response = service.findAll(pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionGetResponse> findById(@PathVariable UUID id) {
        var response = service.findById(id);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionPutResponse> update(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid PrescriptionPutRequest request) {

        authService.authenticatedUser(pin);

        var response = service.update(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin) {

        authService.authenticatedUser(pin);

        service.delete(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<PrescriptionItemGetResponse> addItem(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid PrescriptionItemPostRequest request) {

        authService.authenticatedUser(pin);

        var response = service.addItem(id, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<PrescriptionItemGetResponse> updateItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid PrescriptionItemPutRequest request) {

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

    @PatchMapping("/{id}/items/{itemId}/mark-as-received")
    public ResponseEntity<Void> markItemAsReceived(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markItemAsReceived(id, itemId, actor);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/mark-as-received")
    public ResponseEntity<Void> markAllAsReceived(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markAllAsReceived(id, actor);

        return ResponseEntity.noContent().build();
    }
}
