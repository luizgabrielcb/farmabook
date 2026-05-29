package br.com.luizgabriel.farmaorder.auth.domain;

import br.com.luizgabriel.farmaorder.auth.dto.*;
import br.com.luizgabriel.farmaorder.auth.exception.NotFoundException;
import br.com.luizgabriel.farmaorder.auth.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserPostResponse save(UserPostRequest userPostRequest) {
        validateNameAlreadyInUse(userPostRequest.name());

        validatePinAlreadyInUse(userPostRequest.pin());

        var pinHash = passwordEncoder.encode(userPostRequest.pin());

        var user = userMapper.toUser(userPostRequest, pinHash);

        var savedUser = userRepository.save(user);

        return userMapper.toUserPostResponse(savedUser);
    }

    public Page<UserGetResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toUserGetResponse);
    }

    public UserGetResponse findById(UUID id) {
        var user = findByIdOrThrowNotFoundException(id);

        return userMapper.toUserGetResponse(user);
    }

    public UserPutResponse update(UUID id, UserPutRequest userPutRequest) {
        var user = findByIdOrThrowNotFoundException(id);

        user.setName(userPutRequest.name());
        user.setRole(userPutRequest.role());

        var updatedUser = userRepository.save(user);

        return userMapper.toUserPutResponse(updatedUser);
    }

    public void delete(UUID id) {
        var user = findByIdOrThrowNotFoundException(id);

        userRepository.delete(user);
    }

    public void activate(UUID id) {
        var user = findByIdOrThrowNotFoundException(id);

        user.setActive(true);

        userRepository.save(user);
    }

    public void deactivate(UUID id) {
        var user = findByIdOrThrowNotFoundException(id);

        user.setActive(false);

        userRepository.save(user);
    }

    public UserGetResponse validatePin(String pin) {
        var user = findUserByPinOrThrow(pin);

        return userMapper.toUserGetResponse(user);
    }

    public void changePin(String currentPin, String newPin) {
        var user = findUserByPinOrThrow(currentPin);

        validatePinAlreadyInUse(newPin);

        user.setPinHash(passwordEncoder.encode(newPin));

        userRepository.save(user);
    }

    private User findByIdOrThrowNotFoundException(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id " + id + " not found"));
    }

    private User findUserByPinOrThrow(String pin) {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(pin, u.getPinHash()))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
    }

    private void validatePinAlreadyInUse(String pin) {
        userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(pin, u.getPinHash()))
                .findFirst()
                .ifPresent(u -> {
                    throw new UnauthorizedException("This pin has already in use by another user");
                });
    }

    private void validateNameAlreadyInUse(String name) {
        userRepository.findByNameIgnoreCase(name)
                .ifPresent(u -> {
                    throw new UnauthorizedException("This name has already in use by another user");
                });
    }
}
