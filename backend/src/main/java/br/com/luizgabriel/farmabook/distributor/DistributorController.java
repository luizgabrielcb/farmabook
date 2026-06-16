package br.com.luizgabriel.farmabook.distributor;
import br.com.luizgabriel.farmabook.auth.AuthService;
import br.com.luizgabriel.farmabook.distributor.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
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
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @PostMapping
    public ResponseEntity<DistributorGetResponse> save(
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid DistributorPostRequest request) {
        authService.authenticatedUser(pin);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DistributorGetResponse> update(
            @PathVariable UUID id,
            @RequestHeader("X-Auth-Pin") String pin,
            @RequestBody @Valid DistributorPostRequest request) {
        authService.authenticatedUser(pin);
        return ResponseEntity.ok(service.update(id, request));
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
