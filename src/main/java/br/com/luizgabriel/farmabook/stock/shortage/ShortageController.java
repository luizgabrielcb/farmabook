package br.com.luizgabriel.farmabook.stock.shortage;

import br.com.luizgabriel.farmabook.auth.AuthService;
import br.com.luizgabriel.farmabook.stock.shortage.dto.*;
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
@RequestMapping("/shortages")
@RequiredArgsConstructor
public class ShortageController {

    private final ShortageService service;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<ShortagePostResponse> save(@RequestHeader("X-Auth-Pin") String pin,
                                                     @RequestBody @Valid ShortagePostRequest request) {
        var actor = authService.authenticatedUser(pin);

        var shortagePostResponse = service.save(request, actor);

        return ResponseEntity.status(HttpStatus.CREATED).body(shortagePostResponse);
    }

    @GetMapping
    public ResponseEntity<Page<ShortageGetResponse>> findAll(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        var shortageGetResponsePage = service.findAll(pageable);

        return ResponseEntity.ok(shortageGetResponsePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShortageGetResponse> findById(@PathVariable UUID id) {
        var shortageGetResponse = service.findById(id);

        return ResponseEntity.ok(shortageGetResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShortagePutResponse> update(
            @PathVariable UUID id, @RequestHeader("X-Auth-Pin") String pin, @RequestBody @Valid ShortagePutRequest request) {

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
    public ResponseEntity<Void> markAsOrdered(@PathVariable UUID id, @RequestHeader("X-Auth-Pin") String pin) {
        var actor = authService.authenticatedUser(pin);

        service.markAsOrdered(id, actor);

        return ResponseEntity.noContent().build();
    }
}
