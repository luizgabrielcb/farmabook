package br.com.luizgabriel.farmabook.auth;

import br.com.luizgabriel.farmabook.commons.FileUtils;
import br.com.luizgabriel.farmabook.config.IntegrationTestConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(value = "/sql/user/delete-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class UserControllerTestIT extends IntegrationTestConfig {

    private static final String URL = "/users";
    private static final String AUTH_PIN = "1234";
    private static final UUID NONEXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-999999999999");

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("POST /users should return 201 and persist user when successful")
    void save_ReturnsCreated_WhenSuccessful() {
        var request = fileUtils.readResourceFile("user/post-request-user.json");

        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(201)
                .body("id", Matchers.notNullValue());

        var saved = userRepository.findByNameIgnoreCase("Pedro Gerente");
        assertThat(saved).isPresent();
        assertThat(saved.get().getRole()).isEqualTo(UserRole.SELLER);
        assertThat(saved.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("POST /users should return 400 when required fields are invalid")
    void save_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var request = fileUtils.readResourceFile("user/post-request-user-invalid-fields.json");
        var expected = fileUtils.readResourceFile("user/post-response-user-invalid-fields-400.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(400)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /users should return 409 when PIN is already in use")
    void save_ReturnsConflict_WhenPinAlreadyInUse() {
        var request = fileUtils.readResourceFile("user/post-request-user-conflict-pin.json");
        var expected = fileUtils.readResourceFile("user/post-response-user-conflict-pin-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/user/insert-one-user.sql")
    @DisplayName("POST /users should return 409 when name is already in use")
    void save_ReturnsConflict_WhenNameAlreadyInUse() {
        var request = fileUtils.readResourceFile("user/post-request-user-duplicate-name.json");
        var expected = fileUtils.readResourceFile("user/post-response-user-conflict-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/user/insert-two-users.sql")
    @DisplayName("GET /users should return 200 with a page of users when successful")
    void findAll_ReturnsPageOfUsers_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("user/get-response-user-list-200.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .get(URL)
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("content[*].id", "content[*].createdAt", "content[*].updatedAt",
                        "pageable", "last", "first", "size", "number", "sort", "numberOfElements", "empty")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /users should return 200 with only the seed user when no extra users are inserted")
    void findAll_ReturnsOneSeedUser_WhenNoExtraUsersInserted() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .get(URL)
                .then()
                .log().all()
                .statusCode(200)
                .body("totalElements", Matchers.equalTo(1));
    }

    @Test
    @Sql("/sql/user/insert-one-user.sql")
    @DisplayName("GET /users/{id} should return 200 with user when found")
    void findById_ReturnsUser_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("user/get-response-user-by-id-200.json");
        var user = userRepository.findByNameIgnoreCase("João Vendedor").orElseThrow();

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .get(URL + "/" + user.getId())
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /users/{id} should return 404 when user is not found")
    void findById_ReturnsNotFound_WhenUserNotFound() {
        var expected = fileUtils.readResourceFile("user/get-response-user-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .get(URL + "/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/user/insert-one-user.sql")
    @DisplayName("PUT /users/{id} should return 200 and update user when successful")
    void update_ReturnsOk_WhenSuccessful() {
        var request = fileUtils.readResourceFile("user/put-request-user.json");
        var expected = fileUtils.readResourceFile("user/put-response-user-200.json");
        var user = userRepository.findByNameIgnoreCase("João Vendedor").orElseThrow();

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + user.getId())
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "updatedAt")
                .isEqualTo(expected);

        var updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("João Gerente");
        assertThat(updated.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @Sql("/sql/user/insert-one-user.sql")
    @DisplayName("PUT /users/{id} should return 400 when required fields are invalid")
    void update_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var expected = fileUtils.readResourceFile("user/post-response-user-invalid-fields-400.json");
        var user = userRepository.findByNameIgnoreCase("João Vendedor").orElseThrow();

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body("{\"name\": \"\", \"role\": null}")
                .when()
                .put(URL + "/" + user.getId())
                .then()
                .log().all()
                .statusCode(400)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("PUT /users/{id} should return 404 when user is not found")
    void update_ReturnsNotFound_WhenUserNotFound() {
        var request = fileUtils.readResourceFile("user/put-request-user.json");
        var expected = fileUtils.readResourceFile("user/put-response-user-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/user/insert-two-users.sql")
    @DisplayName("PUT /users/{id} should return 409 when name is already in use by another user")
    void update_ReturnsConflict_WhenNameAlreadyInUseByAnotherUser() {
        var expected = fileUtils.readResourceFile("user/put-response-user-conflict-409.json");
        var user = userRepository.findByNameIgnoreCase("João Vendedor").orElseThrow();

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Maria Vendedora\", \"role\": \"SELLER\"}")
                .when()
                .put(URL + "/" + user.getId())
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/user/insert-one-user.sql")
    @DisplayName("DELETE /users/{id} should return 204 and soft-delete user when successful")
    void delete_ReturnsNoContent_WhenSuccessful() {
        var user = userRepository.findByNameIgnoreCase("João Vendedor").orElseThrow();

        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .delete(URL + "/" + user.getId())
                .then()
                .log().all()
                .statusCode(204);

        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /users/{id} should return 404 when user is not found")
    void delete_ReturnsNotFound_WhenUserNotFound() {
        var expected = fileUtils.readResourceFile("user/delete-response-user-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .delete(URL + "/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/user/insert-one-inactive-user.sql")
    @DisplayName("PATCH /users/{id}/activate should return 204 and set user active when successful")
    void activate_ReturnsNoContent_WhenSuccessful() {
        var user = userRepository.findByNameIgnoreCase("João Inativo").orElseThrow();

        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + user.getId() + "/activate")
                .then()
                .log().all()
                .statusCode(204);

        var updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    @DisplayName("PATCH /users/{id}/activate should return 404 when user is not found")
    void activate_ReturnsNotFound_WhenUserNotFound() {
        var expected = fileUtils.readResourceFile("user/patch-response-user-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/activate")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/user/insert-one-user.sql")
    @DisplayName("PATCH /users/{id}/deactivate should return 204 and set user inactive when successful")
    void deactivate_ReturnsNoContent_WhenSuccessful() {
        var user = userRepository.findByNameIgnoreCase("João Vendedor").orElseThrow();

        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + user.getId() + "/deactivate")
                .then()
                .log().all()
                .statusCode(204);

        var updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    @DisplayName("PATCH /users/{id}/deactivate should return 404 when user is not found")
    void deactivate_ReturnsNotFound_WhenUserNotFound() {
        var expected = fileUtils.readResourceFile("user/patch-response-user-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/deactivate")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }
}
