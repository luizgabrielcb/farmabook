package br.com.luizgabriel.farmabook.shortage;

import br.com.luizgabriel.farmabook.auth.AuthService;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageOrderGetResponse;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageOrderItemRequest;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageOrderListResponse;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageOrderPostRequest;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageOrderPutRequest;
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
@RequestMapping("/shortage-orders")
@RequiredArgsConstructor
public class ShortageOrderController {

    private final ShortageOrderService service;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<ShortageOrderGetResponse> save(
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid ShortageOrderPostRequest request) {
        var actor = authService.authenticatedUser(pin);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request, actor));
    }

    @GetMapping
    public ResponseEntity<Page<ShortageOrderListResponse>> findAll(
            @RequestParam ShortageType shortageType,
            @RequestParam(required = false) UUID distributorId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.findAll(shortageType, distributorId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShortageOrderGetResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShortageOrderGetResponse> update(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid ShortageOrderPutRequest request) {
        authService.authenticatedUser(pin);
        return ResponseEntity.ok(service.update(id, request));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ShortageOrderGetResponse> addItem(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid ShortageOrderItemRequest request) {
        var actor = authService.authenticatedUser(pin);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addItem(id, request, actor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestHeader("X-Auth-Pin") String pin) {
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
}
