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

        var response = service.save(request, actor);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ShortageOrderListResponse>> findAll(
            @RequestParam ShortageType shortageType,
            @RequestParam(required = false) UUID distributorId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        var response = service.findAll(shortageType, distributorId, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShortageOrderGetResponse> findById(@PathVariable UUID id) {
        var response = service.findById(id);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShortageOrderGetResponse> update(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid ShortageOrderPutRequest request) {

        authService.authenticatedUser(pin);

        var response = service.update(id, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ShortageOrderGetResponse> addItem(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid ShortageOrderItemRequest request) {

        var actor = authService.authenticatedUser(pin);

        var response = service.addItem(id, request, actor);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
