package br.com.luizgabriel.farmabook.auth;

import br.com.luizgabriel.farmabook.commons.FileUtils;
import br.com.luizgabriel.farmabook.config.IntegrationTestConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(value = "/sql/clean-database.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class AuthControllerTestIT extends IntegrationTestConfig {

    private static final String URL = "/auth";

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /auth/validate-pin should return 200 with user info when PIN is valid")
    void validatePin_ReturnsOk_WhenPinIsValid() {
        var request = fileUtils.readResourceFile("auth/post-request-validate-pin.json");
        var expected = fileUtils.readResourceFile("auth/post-response-validate-pin-200.json");

        var body = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/validate-pin")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /auth/validate-pin should return 400 when PIN format is invalid")
    void validatePin_ReturnsBadRequest_WhenPinFormatIsInvalid() {
        var request = fileUtils.readResourceFile("auth/post-request-validate-pin-invalid.json");
        var expected = fileUtils.readResourceFile("auth/post-response-validate-pin-invalid-400.json");

        var body = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/validate-pin")
                .then()
                .log().all()
                .statusCode(400)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /auth/validate-pin should return 401 when PIN does not match any user")
    void validatePin_ReturnsUnauthorized_WhenPinIsWrong() {
        var expected = fileUtils.readResourceFile("auth/post-response-validate-pin-unauthorized-401.json");

        var body = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"pin\": \"9999\"}")
                .when()
                .post(URL + "/validate-pin")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /auth/change-pin should return 204 and update user PIN when successful")
    void changePin_ReturnsNoContent_WhenSuccessful() {
        var request = fileUtils.readResourceFile("auth/post-request-change-pin.json");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/change-pin")
                .then()
                .log().all()
                .statusCode(204);

        var user = userRepository.findByNameIgnoreCase("User Teste").orElseThrow();
        assertThat(passwordEncoder.matches("5678", user.getPinHash())).isTrue();
        assertThat(passwordEncoder.matches("1234", user.getPinHash())).isFalse();
    }

    @Test
    @DisplayName("POST /auth/change-pin should return 400 when required fields are invalid")
    void changePin_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var request = fileUtils.readResourceFile("auth/post-request-change-pin-invalid.json");
        var expected = fileUtils.readResourceFile("auth/post-response-change-pin-invalid-400.json");

        var body = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/change-pin")
                .then()
                .log().all()
                .statusCode(400)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /auth/change-pin should return 401 when current PIN does not match any user")
    void changePin_ReturnsUnauthorized_WhenCurrentPinIsWrong() {
        var expected = fileUtils.readResourceFile("auth/post-response-change-pin-unauthorized-401.json");

        var body = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"currentPin\": \"9999\", \"newPin\": \"5678\"}")
                .when()
                .post(URL + "/change-pin")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /auth/change-pin should return 409 when new PIN is the same as current PIN")
    void changePin_ReturnsConflict_WhenNewPinSameAsCurrent() {
        var expected = fileUtils.readResourceFile("auth/post-response-change-pin-same-pin-conflict-409.json");

        var body = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"currentPin\": \"1234\", \"newPin\": \"1234\"}")
                .when()
                .post(URL + "/change-pin")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /auth/change-pin should return 409 when new PIN is already in use by another user")
    void changePin_ReturnsConflict_WhenNewPinAlreadyInUse() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(fileUtils.readResourceFile("user/post-request-user.json"))
                .when()
                .post("/users")
                .then()
                .statusCode(201);

        var expected = fileUtils.readResourceFile("auth/post-response-change-pin-pin-in-use-conflict-409.json");

        var body = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(fileUtils.readResourceFile("auth/post-request-change-pin.json"))
                .when()
                .post(URL + "/change-pin")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }
}
