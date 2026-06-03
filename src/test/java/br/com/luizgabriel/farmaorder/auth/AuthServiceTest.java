package br.com.luizgabriel.farmaorder.auth;

import br.com.luizgabriel.farmaorder.commons.UserUtils;
import br.com.luizgabriel.farmaorder.exception.ConflictException;
import br.com.luizgabriel.farmaorder.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService service;

    @InjectMocks
    private UserUtils utils;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("validatePin should return UserGetResponse when PIN matches an active user")
    void validatePin_ReturnsUserGetResponse_WhenPinMatches() {
        var pin = "1234";
        var user = utils.newUser();
        var response = utils.newUserGetResponse(user);

        BDDMockito.when(userRepository.findAll()).thenReturn(List.of(user));
        BDDMockito.when(passwordEncoder.matches(pin, user.getPinHash())).thenReturn(true);
        BDDMockito.when(userMapper.toUserGetResponse(user)).thenReturn(response);

        var result = service.validatePin(pin);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("validatePin should throw UnauthorizedException when PIN does not match any active user")
    void validatePin_ThrowsUnauthorizedException_WhenPinDoesNotMatch() {
        var pin = "9999";

        BDDMockito.when(userRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.validatePin(pin))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("authenticatedUser should return User when PIN matches an active user")
    void authenticatedUser_ReturnsUser_WhenPinMatches() {
        var pin = "1234";
        var user = utils.newUser();

        BDDMockito.when(userRepository.findAll()).thenReturn(List.of(user));
        BDDMockito.when(passwordEncoder.matches(pin, user.getPinHash())).thenReturn(true);

        var result = service.authenticatedUser(pin);

        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("authenticatedUser should throw UnauthorizedException when PIN does not match any active user")
    void authenticatedUser_ThrowsUnauthorizedException_WhenPinDoesNotMatch() {
        var pin = "9999";

        BDDMockito.when(userRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.authenticatedUser(pin))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("changePin should update pin hash and save user when successful")
    void changePin_UpdatesPinHash_WhenSuccessful() {
        var currentPin = "1234";
        var newPin = "5678";
        var newPinHash = "$2a$04$new_hash";
        var user = utils.newUser();

        BDDMockito.when(userRepository.findAll()).thenReturn(List.of(user));
        BDDMockito.when(passwordEncoder.matches(currentPin, user.getPinHash())).thenReturn(true);
        BDDMockito.when(passwordEncoder.encode(newPin)).thenReturn(newPinHash);
        BDDMockito.when(userRepository.save(user)).thenReturn(user);

        service.changePin(currentPin, newPin);

        assertThat(user.getPinHash()).isEqualTo(newPinHash);
        BDDMockito.then(userRepository).should().save(user);
    }

    @Test
    @DisplayName("changePin should throw ConflictException when current PIN equals new PIN")
    void changePin_ThrowsConflictException_WhenCurrentPinEqualsNewPin() {
        var pin = "1234";

        assertThatThrownBy(() -> service.changePin(pin, pin))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(userRepository).should(Mockito.never()).save(ArgumentMatchers.any(User.class));
    }

    @Test
    @DisplayName("changePin should throw UnauthorizedException when current PIN does not match any active user")
    void changePin_ThrowsUnauthorizedException_WhenCurrentPinDoesNotMatch() {
        var currentPin = "1234";
        var newPin = "5678";

        BDDMockito.when(userRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.changePin(currentPin, newPin))
                .isInstanceOf(UnauthorizedException.class);

        BDDMockito.then(userRepository).should(Mockito.never()).save(ArgumentMatchers.any(User.class));
    }

    @Test
    @DisplayName("changePin should throw ConflictException when new PIN is already in use by an active user")
    void changePin_ThrowsConflictException_WhenNewPinAlreadyInUse() {
        var currentPin = "1234";
        var newPin = "5678";
        var user = utils.newUser();

        BDDMockito.when(userRepository.findAll()).thenReturn(List.of(user));
        BDDMockito.when(passwordEncoder.matches(currentPin, user.getPinHash())).thenReturn(true);
        BDDMockito.when(passwordEncoder.matches(newPin, user.getPinHash())).thenReturn(true);

        assertThatThrownBy(() -> service.changePin(currentPin, newPin))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(userRepository).should(Mockito.never()).save(ArgumentMatchers.any(User.class));
    }

    @Test
    @DisplayName("assertPinNotInUse should not throw when PIN is not in use")
    void assertPinNotInUse_DoesNotThrow_WhenPinIsNotInUse() {
        var pin = "1234";

        BDDMockito.when(userRepository.findAll()).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> service.assertPinNotInUse(pin));
    }

    @Test
    @DisplayName("assertPinNotInUse should throw ConflictException when PIN is already in use")
    void assertPinNotInUse_ThrowsConflictException_WhenPinIsAlreadyInUse() {
        var pin = "1234";
        var user = utils.newUser();

        BDDMockito.when(userRepository.findAll()).thenReturn(List.of(user));
        BDDMockito.when(passwordEncoder.matches(pin, user.getPinHash())).thenReturn(true);

        assertThatThrownBy(() -> service.assertPinNotInUse(pin))
                .isInstanceOf(ConflictException.class);
    }
}
