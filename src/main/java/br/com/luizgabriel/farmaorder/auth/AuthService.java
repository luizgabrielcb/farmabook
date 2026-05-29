package br.com.luizgabriel.farmaorder.auth;

import br.com.luizgabriel.farmaorder.auth.dto.UserGetResponse;
import br.com.luizgabriel.farmaorder.exception.ConflictException;
import br.com.luizgabriel.farmaorder.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserGetResponse validatePin(String pin) {
        var user = findActiveUserMatchingPin(pin)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        return userMapper.toUserGetResponse(user);
    }

    public void changePin(String currentPin, String newPin) {
        if (currentPin.equals(newPin)) {
            throw new ConflictException("New PIN must be different from current PIN");
        }

        var user = findActiveUserMatchingPin(currentPin)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        assertPinNotInUse(newPin);

        user.setPinHash(passwordEncoder.encode(newPin));

        userRepository.save(user);
    }

    public void assertPinNotInUse(String pin) {
        findActiveUserMatchingPin(pin).ifPresent(u -> {
            throw new ConflictException("Choose a different PIN");
        });
    }

    private Optional<User> findActiveUserMatchingPin(String pin) {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(pin, u.getPinHash()))
                .findFirst();
    }
}
