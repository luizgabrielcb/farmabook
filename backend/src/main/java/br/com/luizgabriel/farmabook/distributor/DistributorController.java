package br.com.luizgabriel.farmabook.distributor;

import br.com.luizgabriel.farmabook.auth.AuthService;
import br.com.luizgabriel.farmabook.distributor.dto.*;
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
@RequestMapping("/distributors")
@RequiredArgsConstructor
public class DistributorController {

    private final DistributorService service;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<Page<DistributorGetResponse>> findAll(
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        var response = service.findAll(pageable);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<DistributorGetResponse> save(
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid DistributorPostRequest request) {

        authService.authenticatedUser(pin);

        var response = service.save(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DistributorGetResponse> update(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid DistributorPostRequest request) {

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
}
