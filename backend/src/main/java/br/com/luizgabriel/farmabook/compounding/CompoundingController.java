package br.com.luizgabriel.farmabook.compounding;

import br.com.luizgabriel.farmabook.auth.AuthService;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingGetResponse;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingPostRequest;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingPostResponse;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingPutRequest;
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
@RequestMapping("/compoundings")
@RequiredArgsConstructor
public class CompoundingController {

    private final CompoundingService service;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<Page<CompoundingGetResponse>> findAll(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        var response = service.findAll(pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompoundingGetResponse> findById(@PathVariable UUID id) {
        var response = service.findById(id);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CompoundingPostResponse> save(
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid CompoundingPostRequest request) {

        var actor = authService.authenticatedUser(pin);

        var response = service.save(request, actor);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompoundingGetResponse> update(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid CompoundingPutRequest request) {

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

    @PatchMapping("/{id}/mark-as-ordered")
    public ResponseEntity<Void> markAsOrdered(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markAsOrdered(id, actor);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/mark-as-received")
    public ResponseEntity<Void> markAsReceived(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markAsReceived(id, actor);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/mark-as-delivered")
    public ResponseEntity<Void> markAsDelivered(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markAsDelivered(id, actor);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/payment/mark-as-paid")
    public ResponseEntity<Void> markAsPaid(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markAsPaid(id, actor);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/payment/mark-as-to-pay")
    public ResponseEntity<Void> markAsToPay(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markAsToPay(id, actor);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/payment/mark-as-make-note")
    public ResponseEntity<Void> markAsMakeNote(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markAsMakeNote(id, actor);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/payment/mark-as-noted")
    public ResponseEntity<Void> markAsNoted(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin) {

        var actor = authService.authenticatedUser(pin);

        service.markAsNoted(id, actor);

        return ResponseEntity.noContent().build();
    }
}
