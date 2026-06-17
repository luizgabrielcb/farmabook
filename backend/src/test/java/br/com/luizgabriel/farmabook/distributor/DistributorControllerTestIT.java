package br.com.luizgabriel.farmabook.distributor;

import br.com.luizgabriel.farmabook.commons.FileUtils;
import br.com.luizgabriel.farmabook.config.AuthenticatedIntegrationConfig;
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

@Sql(value = "/sql/clean-database.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class DistributorControllerTestIT extends AuthenticatedIntegrationConfig {

    private static final String URL = "/distributors";
    private static final UUID DISTRIBUTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID NONEXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-999999999999");

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private DistributorRepository distributorRepository;

    @Test
    @Sql("/sql/distributor/insert-one-distributor.sql")
    @DisplayName("GET /distributors should return 200 with a page when successful")
    void findAll_ReturnsPageOfDistributors_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("distributor/get-response-distributor-list-200.json");

        var body = RestAssured.given()
                .when()
                .get(URL)
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("content[*].id", "content[*].createdAt",
                        "pageable", "last", "first", "size", "number", "sort",
                        "numberOfElements", "empty")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /distributors should return 201 and persist distributor when successful")
    void save_ReturnsCreated_WhenSuccessful() {
        var request = fileUtils.readResourceFile("distributor/post-request-distributor.json");

        RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(201)
                .body("id", Matchers.notNullValue());

        var saved = distributorRepository.findAll().stream()
                .filter(d -> d.getName().equals("Distribuidora Nova"))
                .findFirst();
        assertThat(saved).isPresent();
    }

    @Test
    @DisplayName("POST /distributors should return 400 when required fields are invalid")
    void save_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var expected = fileUtils.readResourceFile("distributor/post-response-distributor-invalid-fields-400.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body("{\"name\": \"\"}")
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(400)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /distributors should return 401 when PIN is invalid")
    void save_ReturnsUnauthorized_WhenPinIsInvalid() {
        var request = fileUtils.readResourceFile("distributor/post-request-distributor.json");
        var expected = fileUtils.readResourceFile("distributor/post-response-distributor-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/distributor/insert-one-distributor.sql")
    @DisplayName("PUT /distributors/{id} should return 200 and update distributor when successful")
    void update_ReturnsOk_WhenSuccessful() {
        var request = fileUtils.readResourceFile("distributor/put-request-distributor.json");
        var expected = fileUtils.readResourceFile("distributor/put-response-distributor-200.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + DISTRIBUTOR_ID)
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "createdAt")
                .isEqualTo(expected);

        var updated = distributorRepository.findById(DISTRIBUTOR_ID).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Distribuidora Norte");
    }

    @Test
    @DisplayName("PUT /distributors/{id} should return 404 when distributor is not found")
    void update_ReturnsNotFound_WhenNotFound() {
        var request = fileUtils.readResourceFile("distributor/put-request-distributor.json");
        var expected = fileUtils.readResourceFile("distributor/put-response-distributor-not-found-404.json");

        var body = RestAssured.given()
                .header(authPinHeader())
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
    @DisplayName("PUT /distributors/{id} should return 401 when PIN is invalid")
    void update_ReturnsUnauthorized_WhenPinIsInvalid() {
        var request = fileUtils.readResourceFile("distributor/put-request-distributor.json");
        var expected = fileUtils.readResourceFile("distributor/post-response-distributor-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + DISTRIBUTOR_ID)
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/distributor/insert-one-distributor.sql")
    @DisplayName("DELETE /distributors/{id} should return 204 and soft-delete distributor when successful")
    void delete_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .delete(URL + "/" + DISTRIBUTOR_ID)
                .then()
                .log().all()
                .statusCode(204);

        assertThat(distributorRepository.findById(DISTRIBUTOR_ID)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /distributors/{id} should return 404 when distributor is not found")
    void delete_ReturnsNotFound_WhenNotFound() {
        var expected = fileUtils.readResourceFile("distributor/delete-response-distributor-not-found-404.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .delete(URL + "/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("DELETE /distributors/{id} should return 401 when PIN is invalid")
    void delete_ReturnsUnauthorized_WhenPinIsInvalid() {
        var expected = fileUtils.readResourceFile("distributor/post-response-distributor-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .when()
                .delete(URL + "/" + DISTRIBUTOR_ID)
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }
}
