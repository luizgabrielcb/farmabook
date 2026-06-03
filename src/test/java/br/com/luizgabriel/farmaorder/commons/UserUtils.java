package br.com.luizgabriel.farmaorder.commons;

import br.com.luizgabriel.farmaorder.auth.User;
import br.com.luizgabriel.farmaorder.auth.UserRole;
import br.com.luizgabriel.farmaorder.auth.dto.*;

import java.time.Instant;
import java.util.UUID;

public class UserUtils {

    public static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    public User newUser() {
        return User.builder()
                .id(USER_ID)
                .name("Test User")
                .pinHash("$2a$04$hashed_pin")
                .role(UserRole.SELLER)
                .active(true)
                .build();
    }

    public User newInactiveUser() {
        return User.builder()
                .id(USER_ID)
                .name("Test User")
                .pinHash("$2a$04$hashed_pin")
                .role(UserRole.SELLER)
                .active(false)
                .build();
    }

    public User newOtherUser() {
        return User.builder()
                .id(OTHER_USER_ID)
                .name("Other User")
                .pinHash("$2a$04$other_hashed_pin")
                .role(UserRole.SELLER)
                .active(true)
                .build();
    }

    public UserPostRequest newUserPostRequest() {
        return new UserPostRequest("Test User", "1234", UserRole.SELLER);
    }

    public UserPostResponse newUserPostResponse(User user) {
        return new UserPostResponse(user.getId(), user.getName(), user.getRole(), user.isActive(), Instant.now());
    }

    public UserGetResponse newUserGetResponse(User user) {
        return new UserGetResponse(user.getId(), user.getName(), user.getRole(), user.isActive(), Instant.now(), Instant.now());
    }

    public UserPutRequest newUserPutRequest() {
        return new UserPutRequest("Updated User", UserRole.ADMIN);
    }

    public UserPutResponse newUserPutResponse(User user) {
        return new UserPutResponse(user.getId(), user.getName(), user.getRole(), Instant.now());
    }
}
