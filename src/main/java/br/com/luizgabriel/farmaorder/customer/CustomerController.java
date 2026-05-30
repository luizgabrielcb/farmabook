package br.com.luizgabriel.farmaorder.customer;

import br.com.luizgabriel.farmaorder.customer.dto.*;
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
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    public ResponseEntity<CustomerPostResponse> save(@RequestBody @Valid CustomerPostRequest customerPostRequest) {
        var customerPostResponse = service.save(customerPostRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(customerPostResponse);
    }

    @GetMapping
    public ResponseEntity<Page<CustomerGetResponse>> findAll(@PageableDefault(sort = "name") Pageable pageable) {
        var customerGetResponsePage = service.findAll(pageable);

        return ResponseEntity.ok(customerGetResponsePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerGetResponse> findById(@PathVariable UUID id) {
        var customerGetResponse = service.findById(id);

        return ResponseEntity.ok(customerGetResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerPutResponse> update(@PathVariable UUID id,
                                                      @RequestBody @Valid CustomerPutRequest customerPutRequest) {
        var customerPutResponse = service.update(id, customerPutRequest);

        return ResponseEntity.ok(customerPutResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);

        return ResponseEntity.noContent().build();
    }
}
