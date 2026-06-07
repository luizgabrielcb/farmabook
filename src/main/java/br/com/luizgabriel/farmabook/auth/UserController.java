package br.com.luizgabriel.farmabook.auth;

import br.com.luizgabriel.farmabook.auth.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    public ResponseEntity<UserPostResponse> save(@RequestBody @Valid UserPostRequest userPostRequest) {
        var userPostResponse = service.save(userPostRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(userPostResponse);
    }

    @GetMapping
    public ResponseEntity<Page<UserGetResponse>> findAll(@PageableDefault(sort = "name") Pageable pageable) {
        var userPostResponsePage = service.findAll(pageable);

        return ResponseEntity.ok(userPostResponsePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserGetResponse> findById(@PathVariable UUID id) {
        var userGetResponse = service.findById(id);

        return ResponseEntity.ok(userGetResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserPutResponse> update(@PathVariable UUID id, @RequestBody @Valid UserPutRequest userPutRequest) {
        var userPutResponse = service.update(id, userPutRequest);

        return ResponseEntity.ok(userPutResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        service.activate(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);

        return ResponseEntity.noContent().build();
    }
}
