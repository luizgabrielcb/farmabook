package br.com.luizgabriel.farmabook.auth;

import br.com.luizgabriel.farmabook.auth.dto.ChangePinRequest;
import br.com.luizgabriel.farmabook.auth.dto.UserGetResponse;
import br.com.luizgabriel.farmabook.auth.dto.ValidatePinRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/validate-pin")
    public ResponseEntity<UserGetResponse> validatePin(@RequestBody @Valid ValidatePinRequest request) {
        var userGetResponse = service.validatePin(request.pin());

        return ResponseEntity.ok(userGetResponse);
    }

    @PostMapping("/change-pin")
    public ResponseEntity<Void> changePin(@RequestBody @Valid ChangePinRequest request) {
        service.changePin(request.currentPin(), request.newPin());

        return ResponseEntity.noContent().build();
    }
}
