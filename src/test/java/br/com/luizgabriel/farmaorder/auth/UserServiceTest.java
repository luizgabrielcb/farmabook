package br.com.luizgabriel.farmaorder.auth;

import br.com.luizgabriel.farmaorder.auth.dto.*;
import br.com.luizgabriel.farmaorder.commons.UserUtils;
import br.com.luizgabriel.farmaorder.exception.ConflictException;
import br.com.luizgabriel.farmaorder.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService service;

    @InjectMocks
    private UserUtils utils;

    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper mapper;

    @Mock
    private AuthService authService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("save should return UserPostResponse when successful")
    void save_ReturnsUserPostResponse_WhenSuccessful() {
        var request = utils.newUserPostRequest();
        var user = utils.newUser();
        var pinHash = "$2a$04$encoded";
        var response = utils.newUserPostResponse(user);

        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.empty());
        BDDMockito.when(passwordEncoder.encode(request.pin())).thenReturn(pinHash);
        BDDMockito.when(mapper.toUser(request, pinHash)).thenReturn(user);
        BDDMockito.when(repository.save(user)).thenReturn(user);
        BDDMockito.when(mapper.toUserPostResponse(user)).thenReturn(response);

        var result = service.save(request);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(user);
    }

    @Test
    @DisplayName("save should throw ConflictException when name is already in use")
    void save_ThrowsConflictException_WhenNameAlreadyInUse() {
        var request = utils.newUserPostRequest();
        var existingUser = utils.newUser();

        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(User.class));
    }

    @Test
    @DisplayName("save should throw ConflictException when PIN is already in use")
    void save_ThrowsConflictException_WhenPinAlreadyInUse() {
        var request = utils.newUserPostRequest();

        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.empty());
        BDDMockito.willThrow(new ConflictException("Choose a different PIN"))
                .given(authService).assertPinNotInUse(request.pin());

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(User.class));
    }

    @Test
    @DisplayName("findAll should return a page of users when successful")
    void findAll_ReturnsPageOfUsers_WhenSuccessful() {
        var pageable = Pageable.ofSize(10);
        var user = utils.newUser();
        var response = utils.newUserGetResponse(user);
        var page = new PageImpl<>(List.of(user));

        BDDMockito.when(repository.findAll(pageable)).thenReturn(page);
        BDDMockito.when(mapper.toUserGetResponse(user)).thenReturn(response);

        var result = service.findAll(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("findAll should return an empty page when no users exist")
    void findAll_ReturnsEmptyPage_WhenNoUsersExist() {
        var pageable = Pageable.ofSize(10);

        BDDMockito.when(repository.findAll(pageable)).thenReturn(Page.empty());

        var result = service.findAll(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findById should return UserGetResponse when user is found")
    void findById_ReturnsUserGetResponse_WhenSuccessful() {
        var user = utils.newUser();
        var response = utils.newUserGetResponse(user);

        BDDMockito.when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        BDDMockito.when(mapper.toUserGetResponse(user)).thenReturn(response);

        var result = service.findById(user.getId());

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("findById should throw NotFoundException when user is not found")
    void findById_ThrowsNotFoundException_WhenUserNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("update should return UserPutResponse when successful")
    void update_ReturnsUserPutResponse_WhenSuccessful() {
        var user = utils.newUser();
        var request = utils.newUserPutRequest();
        var response = utils.newUserPutResponse(user);

        BDDMockito.when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.empty());
        BDDMockito.when(repository.save(user)).thenReturn(user);
        BDDMockito.when(mapper.toUserPutResponse(user)).thenReturn(response);

        var result = service.update(user.getId(), request);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(user);
    }

    @Test
    @DisplayName("update should throw NotFoundException when user is not found")
    void update_ThrowsNotFoundException_WhenUserNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newUserPutRequest();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(User.class));
    }

    @Test
    @DisplayName("update should throw ConflictException when name is already in use by another user")
    void update_ThrowsConflictException_WhenNameAlreadyInUseByAnotherUser() {
        var user = utils.newUser();
        var anotherUser = utils.newOtherUser();
        var request = utils.newUserPutRequest();

        BDDMockito.when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> service.update(user.getId(), request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(User.class));
    }

    @Test
    @DisplayName("update should not throw when name belongs to the same user being updated")
    void update_DoesNotThrow_WhenNameBelongsToSameUser() {
        var user = utils.newUser();
        var request = utils.newUserPutRequest();
        var response = utils.newUserPutResponse(user);

        BDDMockito.when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        BDDMockito.when(repository.findByNameIgnoreCase(request.name())).thenReturn(Optional.of(user));
        BDDMockito.when(repository.save(user)).thenReturn(user);
        BDDMockito.when(mapper.toUserPutResponse(user)).thenReturn(response);

        assertThatNoException().isThrownBy(() -> service.update(user.getId(), request));

        BDDMockito.then(repository).should().save(user);
    }

    @Test
    @DisplayName("delete should delete the user when successful")
    void delete_DeletesUser_WhenSuccessful() {
        var user = utils.newUser();

        BDDMockito.when(repository.findById(user.getId())).thenReturn(Optional.of(user));

        service.delete(user.getId());

        BDDMockito.then(repository).should().delete(user);
    }

    @Test
    @DisplayName("delete should throw NotFoundException when user is not found")
    void delete_ThrowsNotFoundException_WhenUserNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(User.class));
    }

    @Test
    @DisplayName("activate should set user active to true when successful")
    void activate_SetsUserActiveTrue_WhenSuccessful() {
        var user = utils.newInactiveUser();

        BDDMockito.when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        BDDMockito.when(repository.save(user)).thenReturn(user);

        service.activate(user.getId());

        assertThat(user.isActive()).isTrue();
        BDDMockito.then(repository).should().save(user);
    }

    @Test
    @DisplayName("activate should throw NotFoundException when user is not found")
    void activate_ThrowsNotFoundException_WhenUserNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("deactivate should set user active to false when successful")
    void deactivate_SetsUserActiveFalse_WhenSuccessful() {
        var user = utils.newUser();

        BDDMockito.when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        BDDMockito.when(repository.save(user)).thenReturn(user);

        service.deactivate(user.getId());

        assertThat(user.isActive()).isFalse();
        BDDMockito.then(repository).should().save(user);
    }

    @Test
    @DisplayName("deactivate should throw NotFoundException when user is not found")
    void deactivate_ThrowsNotFoundException_WhenUserNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(id))
                .isInstanceOf(NotFoundException.class);
    }
}
