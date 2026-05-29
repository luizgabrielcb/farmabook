package br.com.luizgabriel.farmaorder.auth;

import br.com.luizgabriel.farmaorder.auth.dto.*;
import br.com.luizgabriel.farmaorder.exception.ConflictException;
import br.com.luizgabriel.farmaorder.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserPostResponse save(UserPostRequest userPostRequest) {
        validateNameAlreadyInUse(userPostRequest.name());

        authService.assertPinNotInUse(userPostRequest.pin());

        var pinHash = passwordEncoder.encode(userPostRequest.pin());

        var user = mapper.toUser(userPostRequest, pinHash);

        var savedUser = repository.save(user);

        return mapper.toUserPostResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public Page<UserGetResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toUserGetResponse);
    }

    @Transactional(readOnly = true)
    public UserGetResponse findById(UUID id) {
        var user = findByIdOrThrowNotFoundException(id);

        return mapper.toUserGetResponse(user);
    }

    @Transactional
    public UserPutResponse update(UUID id, UserPutRequest userPutRequest) {
        var user = findByIdOrThrowNotFoundException(id);

        validateNameAlreadyInUse(userPutRequest.name(), id);

        user.setName(userPutRequest.name());
        user.setRole(userPutRequest.role());

        var updatedUser = repository.save(user);

        return mapper.toUserPutResponse(updatedUser);
    }

    @Transactional
    public void delete(UUID id) {
        var user = findByIdOrThrowNotFoundException(id);

        repository.delete(user);
    }

    @Transactional
    public void activate(UUID id) {
        var user = findByIdOrThrowNotFoundException(id);

        user.setActive(true);

        repository.save(user);
    }

    @Transactional
    public void deactivate(UUID id) {
        var user = findByIdOrThrowNotFoundException(id);

        user.setActive(false);

        repository.save(user);
    }

    private User findByIdOrThrowNotFoundException(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id " + id + " not found"));
    }

    private void validateNameAlreadyInUse(String name) {
        repository.findByNameIgnoreCase(name)
                .ifPresent(u -> {
                    throw new ConflictException("Name already in use");
                });
    }

    private void validateNameAlreadyInUse(String name, UUID userId) {
        repository.findByNameIgnoreCase(name)
                .filter(u -> !u.getId().equals(userId))
                .ifPresent(u -> {
                    throw new ConflictException("Name already in use");
                });
    }
}
