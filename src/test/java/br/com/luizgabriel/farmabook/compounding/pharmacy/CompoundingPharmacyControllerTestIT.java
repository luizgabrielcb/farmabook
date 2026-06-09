package br.com.luizgabriel.farmabook.compounding.pharmacy;

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

@Sql(value = "/sql/compounding-pharmacy/delete-compounding-pharmacies.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class CompoundingPharmacyControllerTestIT extends AuthenticatedIntegrationConfig {

    private static final String URL = "/compounding-pharmacies";
    private static final UUID NONEXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-999999999999");

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private CompoundingPharmacyRepository compoundingPharmacyRepository;

    @Test
    @Sql("/sql/compounding-pharmacy/insert-one-compounding-pharmacy.sql")
    @DisplayName("GET /compounding-pharmacies should return 200 with a page when successful")
    void findAll_ReturnsPageOfPharmacies_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("compounding-pharmacy/get-response-compounding-pharmacy-list-200.json");

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
    @DisplayName("POST /compounding-pharmacies should return 201 and persist pharmacy when successful")
    void save_ReturnsCreated_WhenSuccessful() {
        var request = fileUtils.readResourceFile("compounding-pharmacy/post-request-compounding-pharmacy.json");

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

        var saved = compoundingPharmacyRepository.findAll().stream()
                .filter(p -> p.getName().equals("Farmácia Magistral Central"))
                .findFirst();
        assertThat(saved).isPresent();
        assertThat(saved.get().getCity()).isEqualTo("São Paulo");
    }

    @Test
    @DisplayName("POST /compounding-pharmacies should return 400 when required fields are invalid")
    void save_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var expected = fileUtils.readResourceFile("compounding-pharmacy/post-response-compounding-pharmacy-invalid-fields-400.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body("{\"name\": \"\", \"city\": \"\"}")
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(400)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /compounding-pharmacies should return 401 when PIN is invalid")
    void save_ReturnsUnauthorized_WhenPinIsInvalid() {
        var request = fileUtils.readResourceFile("compounding-pharmacy/post-request-compounding-pharmacy.json");
        var expected = fileUtils.readResourceFile("compounding-pharmacy/post-response-compounding-pharmacy-unauthorized-401.json");

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
    @Sql("/sql/compounding-pharmacy/insert-one-compounding-pharmacy.sql")
    @DisplayName("PUT /compounding-pharmacies/{id} should return 200 and update pharmacy when successful")
    void update_ReturnsOk_WhenSuccessful() {
        var request = fileUtils.readResourceFile("compounding-pharmacy/put-request-compounding-pharmacy.json");
        var expected = fileUtils.readResourceFile("compounding-pharmacy/put-response-compounding-pharmacy-200.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/00000000-0000-0000-0000-000000000060")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "updatedAt")
                .isEqualTo(expected);

        var updated = compoundingPharmacyRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000060")).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Farmácia Magistral Norte");
        assertThat(updated.getCity()).isEqualTo("Campinas");
    }

    @Test
    @DisplayName("PUT /compounding-pharmacies/{id} should return 404 when pharmacy is not found")
    void update_ReturnsNotFound_WhenNotFound() {
        var request = fileUtils.readResourceFile("compounding-pharmacy/put-request-compounding-pharmacy.json");
        var expected = fileUtils.readResourceFile("compounding-pharmacy/put-response-compounding-pharmacy-not-found-404.json");

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
    @DisplayName("PUT /compounding-pharmacies/{id} should return 401 when PIN is invalid")
    void update_ReturnsUnauthorized_WhenPinIsInvalid() {
        var request = fileUtils.readResourceFile("compounding-pharmacy/put-request-compounding-pharmacy.json");
        var expected = fileUtils.readResourceFile("compounding-pharmacy/post-response-compounding-pharmacy-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/00000000-0000-0000-0000-000000000060")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding-pharmacy/insert-one-compounding-pharmacy.sql")
    @DisplayName("DELETE /compounding-pharmacies/{id} should return 204 and soft-delete pharmacy when successful")
    void delete_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .delete(URL + "/00000000-0000-0000-0000-000000000060")
                .then()
                .log().all()
                .statusCode(204);

        assertThat(compoundingPharmacyRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000060"))).isEmpty();
    }

    @Test
    @DisplayName("DELETE /compounding-pharmacies/{id} should return 404 when pharmacy is not found")
    void delete_ReturnsNotFound_WhenNotFound() {
        var expected = fileUtils.readResourceFile("compounding-pharmacy/delete-response-compounding-pharmacy-not-found-404.json");

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
    @DisplayName("DELETE /compounding-pharmacies/{id} should return 401 when PIN is invalid")
    void delete_ReturnsUnauthorized_WhenPinIsInvalid() {
        var expected = fileUtils.readResourceFile("compounding-pharmacy/post-response-compounding-pharmacy-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .when()
                .delete(URL + "/00000000-0000-0000-0000-000000000060")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }
}
