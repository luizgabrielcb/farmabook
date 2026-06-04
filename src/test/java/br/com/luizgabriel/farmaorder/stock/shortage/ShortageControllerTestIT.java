package br.com.luizgabriel.farmaorder.stock.shortage;

import br.com.luizgabriel.farmaorder.commons.FileUtils;
import br.com.luizgabriel.farmaorder.config.IntegrationTestConfig;
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

@Sql(value = "/sql/shortage/delete-shortages.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class ShortageControllerTestIT extends IntegrationTestConfig {

    private static final String URL = "/shortages";
    private static final String AUTH_PIN = "1234";
    private static final UUID NONEXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-999999999999");

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private ShortageRepository shortageRepository;

    @Test
    @DisplayName("POST /shortages should return 201 and persist shortage when successful")
    void save_ReturnsCreated_WhenSuccessful() {
        var request = fileUtils.readResourceFile("shortage/post-request-shortage.json");

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

        var saved = shortageRepository.findAll().stream()
                .filter(s -> s.getProduct().equals("Amoxicilina 500mg"))
                .findFirst();
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(ShortageStatus.PENDING);
        assertThat(saved.get().getCreatedByName()).isEqualTo("User Teste");
    }

    @Test
    @DisplayName("POST /shortages should return 401 when PIN is invalid")
    void save_ReturnsUnauthorized_WhenPinIsInvalid() {
        var request = fileUtils.readResourceFile("shortage/post-request-shortage.json");
        var expected = fileUtils.readResourceFile("shortage/post-response-shortage-unauthorized-401.json");

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
    @DisplayName("POST /shortages should return 400 when required fields are invalid")
    void save_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var request = fileUtils.readResourceFile("shortage/post-request-shortage-invalid-fields.json");
        var expected = fileUtils.readResourceFile("shortage/post-response-shortage-invalid-fields-400.json");

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
    @Sql("/sql/shortage/insert-one-shortage.sql")
    @DisplayName("GET /shortages should return 200 with a page of shortages when successful")
    void findAll_ReturnsPageOfShortages_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("shortage/get-response-shortage-list-200.json");

        var body = RestAssured.given()
                .when()
                .get(URL)
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("content[*].id", "content[*].createdById", "content[*].createdAt",
                        "content[*].updatedAt", "pageable", "last", "first", "size", "number", "sort",
                        "numberOfElements", "empty")
                .isEqualTo(expected);
    }

    @Test
    @Sql("/sql/shortage/insert-one-shortage.sql")
    @DisplayName("GET /shortages/{id} should return 200 with shortage when found")
    void findById_ReturnsShortage_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("shortage/get-response-shortage-by-id-200.json");

        var body = RestAssured.given()
                .when()
                .get(URL + "/00000000-0000-0000-0000-000000000001")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "createdById", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /shortages/{id} should return 404 when shortage is not found")
    void findById_ReturnsNotFound_WhenShortageNotFound() {
        var expected = fileUtils.readResourceFile("shortage/get-response-shortage-not-found-404.json");

        var body = RestAssured.given()
                .when()
                .get(URL + "/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/shortage/insert-one-shortage.sql")
    @DisplayName("PUT /shortages/{id} should return 200 and update shortage when successful")
    void update_ReturnsOk_WhenSuccessful() {
        var request = fileUtils.readResourceFile("shortage/put-request-shortage.json");
        var expected = fileUtils.readResourceFile("shortage/put-response-shortage-200.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/00000000-0000-0000-0000-000000000001")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "updatedAt")
                .isEqualTo(expected);

        var updated = shortageRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000001")).orElseThrow();
        assertThat(updated.getProduct()).isEqualTo("Dipirona 1g");
        assertThat(updated.getCategory()).isEqualTo(br.com.luizgabriel.farmaorder.stock.Category.GENERICO);
        assertThat(updated.getQuantity()).isEqualTo(3);
    }

    @Test
    @Sql("/sql/shortage/insert-one-shortage.sql")
    @DisplayName("PUT /shortages/{id} should return 400 when required fields are invalid")
    void update_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var expected = fileUtils.readResourceFile("shortage/post-response-shortage-invalid-fields-400.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body("{\"product\": \"\", \"category\": null}")
                .when()
                .put(URL + "/00000000-0000-0000-0000-000000000001")
                .then()
                .log().all()
                .statusCode(400)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("PUT /shortages/{id} should return 404 when shortage is not found")
    void update_ReturnsNotFound_WhenShortageNotFound() {
        var request = fileUtils.readResourceFile("shortage/put-request-shortage.json");
        var expected = fileUtils.readResourceFile("shortage/put-response-shortage-not-found-404.json");

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
    @Sql("/sql/shortage/insert-one-shortage-ordered.sql")
    @DisplayName("PUT /shortages/{id} should return 409 when shortage is already ordered")
    void update_ReturnsConflict_WhenShortageAlreadyOrdered() {
        var request = fileUtils.readResourceFile("shortage/put-request-shortage.json");
        var expected = fileUtils.readResourceFile("shortage/put-response-shortage-already-ordered-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/00000000-0000-0000-0000-000000000001")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/shortage/insert-one-shortage.sql")
    @DisplayName("DELETE /shortages/{id} should return 204 and soft-delete shortage when successful")
    void delete_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .delete(URL + "/00000000-0000-0000-0000-000000000001")
                .then()
                .log().all()
                .statusCode(204);

        assertThat(shortageRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000001"))).isEmpty();
    }

    @Test
    @DisplayName("DELETE /shortages/{id} should return 404 when shortage is not found")
    void delete_ReturnsNotFound_WhenShortageNotFound() {
        var expected = fileUtils.readResourceFile("shortage/delete-response-shortage-not-found-404.json");

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
    @Sql("/sql/shortage/insert-one-shortage-ordered.sql")
    @DisplayName("DELETE /shortages/{id} should return 409 when shortage is already ordered")
    void delete_ReturnsConflict_WhenShortageAlreadyOrdered() {
        var expected = fileUtils.readResourceFile("shortage/delete-response-shortage-already-ordered-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .delete(URL + "/00000000-0000-0000-0000-000000000001")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/shortage/insert-one-shortage.sql")
    @DisplayName("PATCH /shortages/{id}/mark-as-ordered should return 204 and transition shortage when successful")
    void markAsOrdered_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000001/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(204);

        var updated = shortageRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000001")).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ShortageStatus.ORDERED);
        assertThat(updated.getOrderedByName()).isEqualTo("User Teste");
        assertThat(updated.getOrderedAt()).isNotNull();
    }

    @Test
    @DisplayName("PATCH /shortages/{id}/mark-as-ordered should return 404 when shortage is not found")
    void markAsOrdered_ReturnsNotFound_WhenShortageNotFound() {
        var expected = fileUtils.readResourceFile("shortage/patch-response-shortage-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/shortage/insert-one-shortage-ordered.sql")
    @DisplayName("PATCH /shortages/{id}/mark-as-ordered should return 409 when shortage is already ordered")
    void markAsOrdered_ReturnsConflict_WhenShortageAlreadyOrdered() {
        var expected = fileUtils.readResourceFile("shortage/patch-response-shortage-already-ordered-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000001/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }
}
