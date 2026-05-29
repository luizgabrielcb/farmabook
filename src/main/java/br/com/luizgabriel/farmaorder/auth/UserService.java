package br.com.luizgabriel.farmaorder.auth;

import br.com.luizgabriel.farmaorder.auth.dto.*;
import br.com.luizgabriel.farmaorder.exception.ConflictException;
import br.com.luizgabriel.farmaorder.exception.NotFoundException;
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
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public UserPostResponse save(UserPostRequest userPostRequest) {
        validateNameAlreadyInUse(userPostRequest.name());

        authService.assertPinNotInUse(userPostRequest.pin());

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
        validateNameAlreadyInUse(userPutRequest.name(), id);

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

    private User findByIdOrThrowNotFoundException(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id " + id + " not found"));
    }

    private void validateNameAlreadyInUse(String name) {
        userRepository.findByNameIgnoreCase(name)
                .ifPresent(u -> {
                    throw new ConflictException("Name already in use");
                });
    }

    private void validateNameAlreadyInUse(String name, UUID excludeId) {
        userRepository.findByNameIgnoreCase(name)
                .filter(u -> !u.getId().equals(excludeId))
                .ifPresent(u -> {
                    throw new ConflictException("Name already in use");
                });
    }
}
