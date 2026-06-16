package br.com.luizgabriel.farmabook.compounding;

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

@Sql(value = "/sql/compounding/delete-compoundings.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class CompoundingControllerTestIT extends AuthenticatedIntegrationConfig {

    private static final String URL = "/compoundings";
    private static final UUID NONEXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-999999999999");

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private CompoundingRepository compoundingRepository;

    // ---- GET /compoundings ----

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("GET /compoundings should return 200 with a page when successful")
    void findAll_ReturnsPageOfCompoundings_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("compounding/get-response-compounding-list-200.json");

        var body = RestAssured.given()
                .when()
                .get(URL)
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("content[*].id", "content[*].customerId", "content[*].pharmacyId",
                        "content[*].createdById", "content[*].createdAt", "content[*].updatedAt",
                        "content[*].notifiedAt", "content[*].orderedById", "content[*].orderedByName",
                        "content[*].orderedAt", "content[*].receivedById", "content[*].receivedByName",
                        "content[*].receivedAt", "content[*].deliveredById", "content[*].deliveredByName",
                        "content[*].deliveredAt", "content[*].value", "content[*].observations",
                        "content[*].paymentChangedById", "content[*].paymentChangedByName", "content[*].paymentChangedAt",
                        "pageable", "last", "first", "size", "number", "sort", "numberOfElements", "empty")
                .isEqualTo(expected);
    }

    // ---- GET /compoundings/{id} ----

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("GET /compoundings/{id} should return 200 when found")
    void findById_ReturnsCompounding_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("compounding/get-response-compounding-by-id-200.json");

        var body = RestAssured.given()
                .when()
                .get(URL + "/00000000-0000-0000-0000-000000000070")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "customerId", "pharmacyId", "createdById", "createdAt", "updatedAt",
                        "notifiedAt", "orderedById", "orderedByName", "orderedAt",
                        "receivedById", "receivedByName", "receivedAt",
                        "deliveredById", "deliveredByName", "deliveredAt",
                        "paymentChangedById", "paymentChangedByName", "paymentChangedAt")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /compoundings/{id} should return 404 when not found")
    void findById_ReturnsNotFound_WhenNotFound() {
        var expected = fileUtils.readResourceFile("compounding/get-response-compounding-not-found-404.json");

        var body = RestAssured.given()
                .when()
                .get(URL + "/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    // ---- POST /compoundings ----

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("POST /compoundings should return 201 and persist compounding when successful")
    void save_ReturnsCreated_WhenSuccessful() {
        // delete the one inserted by SQL so we can create fresh
        compoundingRepository.deleteAll();

        var request = fileUtils.readResourceFile("compounding/post-request-compounding.json");

        RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(201)
                .body("id", Matchers.notNullValue())
                .body("status", Matchers.equalTo("PENDING"))
                .body("paymentStatus", Matchers.equalTo("TO_PAY"));
    }

    @Test
    @DisplayName("POST /compoundings should return 400 when required fields are invalid")
    void save_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var expected = fileUtils.readResourceFile("compounding/post-response-compounding-invalid-fields-400.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(400)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /compoundings should return 401 when PIN is invalid")
    void save_ReturnsUnauthorized_WhenPinIsInvalid() {
        var request = fileUtils.readResourceFile("compounding/post-request-compounding.json");
        var expected = fileUtils.readResourceFile("compounding/post-response-compounding-unauthorized-401.json");

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

    // ---- PUT /compoundings/{id} ----

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("PUT /compoundings/{id} should return 200 and update compounding when successful")
    void update_ReturnsOk_WhenSuccessful() {
        var request = fileUtils.readResourceFile("compounding/put-request-compounding.json");

        RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/00000000-0000-0000-0000-000000000070")
                .then()
                .log().all()
                .statusCode(200)
                .body("quantity", Matchers.equalTo(3));

        var updated = compoundingRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000070")).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("PUT /compoundings/{id} should return 404 when not found")
    void update_ReturnsNotFound_WhenNotFound() {
        var request = fileUtils.readResourceFile("compounding/put-request-compounding.json");
        var expected = fileUtils.readResourceFile("compounding/put-response-compounding-not-found-404.json");

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
    @Sql("/sql/compounding/insert-one-compounding-delivered.sql")
    @DisplayName("PUT /compoundings/{id} should return 409 when compounding is DELIVERED")
    void update_ReturnsConflict_WhenDelivered() {
        var request = fileUtils.readResourceFile("compounding/put-request-compounding.json");
        var expected = fileUtils.readResourceFile("compounding/put-response-compounding-delivered-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/00000000-0000-0000-0000-000000000070")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("PUT /compoundings/{id} should return 401 when PIN is invalid")
    void update_ReturnsUnauthorized_WhenPinIsInvalid() {
        var request = fileUtils.readResourceFile("compounding/put-request-compounding.json");
        var expected = fileUtils.readResourceFile("compounding/post-response-compounding-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/00000000-0000-0000-0000-000000000070")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    // ---- DELETE /compoundings/{id} ----

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("DELETE /compoundings/{id} should return 204 and soft-delete when successful")
    void delete_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .delete(URL + "/00000000-0000-0000-0000-000000000070")
                .then()
                .log().all()
                .statusCode(204);

        assertThat(compoundingRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000070"))).isEmpty();
    }

    @Test
    @DisplayName("DELETE /compoundings/{id} should return 404 when not found")
    void delete_ReturnsNotFound_WhenNotFound() {
        var expected = fileUtils.readResourceFile("compounding/delete-response-compounding-not-found-404.json");

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
    @Sql("/sql/compounding/insert-one-compounding-delivered.sql")
    @DisplayName("DELETE /compoundings/{id} should return 409 when compounding is DELIVERED")
    void delete_ReturnsConflict_WhenDelivered() {
        var expected = fileUtils.readResourceFile("compounding/delete-response-compounding-delivered-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .delete(URL + "/00000000-0000-0000-0000-000000000070")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("DELETE /compoundings/{id} should return 401 when PIN is invalid")
    void delete_ReturnsUnauthorized_WhenPinIsInvalid() {
        var expected = fileUtils.readResourceFile("compounding/post-response-compounding-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .when()
                .delete(URL + "/00000000-0000-0000-0000-000000000070")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    // ---- PATCH /compoundings/{id}/mark-as-ordered ----

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("PATCH /compoundings/{id}/mark-as-ordered should return 204 and transition when successful")
    void markAsOrdered_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(204);

        var updated = compoundingRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000070")).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CompoundingStatus.ORDERED);
        assertThat(updated.getOrderedByName()).isEqualTo("User Teste");
        assertThat(updated.getOrderedAt()).isNotNull();
    }

    @Test
    @DisplayName("PATCH /compoundings/{id}/mark-as-ordered should return 404 when not found")
    void markAsOrdered_ReturnsNotFound_WhenNotFound() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-compounding-not-found-404.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-delivered.sql")
    @DisplayName("PATCH /compoundings/{id}/mark-as-ordered should return 409 when DELIVERED")
    void markAsOrdered_ReturnsConflict_WhenDelivered() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-compounding-delivered-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("PATCH /compoundings/{id}/mark-as-ordered should return 401 when PIN is invalid")
    void markAsOrdered_ReturnsUnauthorized_WhenPinIsInvalid() {
        var expected = fileUtils.readResourceFile("compounding/post-response-compounding-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    // ---- PATCH /compoundings/{id}/mark-as-received ----

    @Test
    @Sql("/sql/compounding/insert-one-compounding-ordered.sql")
    @DisplayName("PATCH /compoundings/{id}/mark-as-received should return 204 and transition when successful")
    void markAsReceived_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/mark-as-received")
                .then()
                .log().all()
                .statusCode(204);

        var updated = compoundingRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000070")).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CompoundingStatus.RECEIVED);
        assertThat(updated.getReceivedByName()).isEqualTo("User Teste");
        assertThat(updated.getReceivedAt()).isNotNull();
        assertThat(updated.getNotifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("PATCH /compoundings/{id}/mark-as-received should return 404 when not found")
    void markAsReceived_ReturnsNotFound_WhenNotFound() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-compounding-not-found-404.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/mark-as-received")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("PATCH /compoundings/{id}/mark-as-received should return 409 when PENDING")
    void markAsReceived_ReturnsConflict_WhenPending() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-compounding-mark-received-pending-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/mark-as-received")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-delivered.sql")
    @DisplayName("PATCH /compoundings/{id}/mark-as-received should return 409 when DELIVERED")
    void markAsReceived_ReturnsConflict_WhenDelivered() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-compounding-delivered-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/mark-as-received")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-ordered.sql")
    @DisplayName("PATCH /compoundings/{id}/mark-as-received should return 401 when PIN is invalid")
    void markAsReceived_ReturnsUnauthorized_WhenPinIsInvalid() {
        var expected = fileUtils.readResourceFile("compounding/post-response-compounding-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/mark-as-received")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    // ---- PATCH /compoundings/{id}/mark-as-delivered ----

    @Test
    @Sql("/sql/compounding/insert-one-compounding-received.sql")
    @DisplayName("PATCH /compoundings/{id}/mark-as-delivered should return 204 and transition when successful")
    void markAsDelivered_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/mark-as-delivered")
                .then()
                .log().all()
                .statusCode(204);

        var updated = compoundingRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000070")).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CompoundingStatus.DELIVERED);
        assertThat(updated.getDeliveredByName()).isEqualTo("User Teste");
        assertThat(updated.getDeliveredAt()).isNotNull();
    }

    @Test
    @DisplayName("PATCH /compoundings/{id}/mark-as-delivered should return 404 when not found")
    void markAsDelivered_ReturnsNotFound_WhenNotFound() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-compounding-not-found-404.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/mark-as-delivered")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("PATCH /compoundings/{id}/mark-as-delivered should return 409 when not RECEIVED")
    void markAsDelivered_ReturnsConflict_WhenNotReceived() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-compounding-mark-delivered-not-received-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/mark-as-delivered")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-received.sql")
    @DisplayName("PATCH /compoundings/{id}/mark-as-delivered should return 401 when PIN is invalid")
    void markAsDelivered_ReturnsUnauthorized_WhenPinIsInvalid() {
        var expected = fileUtils.readResourceFile("compounding/post-response-compounding-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/mark-as-delivered")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    // ---- PATCH /compoundings/{id}/payment/mark-as-paid ----

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-paid should return 204 when successful")
    void markAsPaid_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/payment/mark-as-paid")
                .then()
                .log().all()
                .statusCode(204);

        var updated = compoundingRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000070")).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-paid should return 404 when not found")
    void markAsPaid_ReturnsNotFound_WhenNotFound() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-payment-not-found-404.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/payment/mark-as-paid")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-noted.sql")
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-paid should return 409 when NOTED")
    void markAsPaid_ReturnsConflict_WhenNoted() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-payment-noted-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/payment/mark-as-paid")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-paid should return 401 when PIN is invalid")
    void markAsPaid_ReturnsUnauthorized_WhenPinIsInvalid() {
        var expected = fileUtils.readResourceFile("compounding/post-response-compounding-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/payment/mark-as-paid")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    // ---- PATCH /compoundings/{id}/payment/mark-as-make-note ----

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-make-note should return 204 when successful")
    void markAsMakeNote_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/payment/mark-as-make-note")
                .then()
                .log().all()
                .statusCode(204);

        var updated = compoundingRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000070")).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.MAKE_NOTE);
    }

    @Test
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-make-note should return 404 when not found")
    void markAsMakeNote_ReturnsNotFound_WhenNotFound() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-payment-not-found-404.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/payment/mark-as-make-note")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-paid.sql")
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-make-note should return 409 when PAID")
    void markAsMakeNote_ReturnsConflict_WhenPaid() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-payment-paid-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/payment/mark-as-make-note")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-noted.sql")
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-make-note should return 409 when NOTED")
    void markAsMakeNote_ReturnsConflict_WhenNoted() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-payment-noted-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/payment/mark-as-make-note")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-make-note should return 401 when PIN is invalid")
    void markAsMakeNote_ReturnsUnauthorized_WhenPinIsInvalid() {
        var expected = fileUtils.readResourceFile("compounding/post-response-compounding-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/payment/mark-as-make-note")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    // ---- PATCH /compoundings/{id}/payment/mark-as-noted ----

    @Test
    @Sql("/sql/compounding/insert-one-compounding-make-note.sql")
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-noted should return 204 when successful")
    void markAsNoted_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/payment/mark-as-noted")
                .then()
                .log().all()
                .statusCode(204);

        var updated = compoundingRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000070")).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.NOTED);
    }

    @Test
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-noted should return 404 when not found")
    void markAsNoted_ReturnsNotFound_WhenNotFound() {
        var expected = fileUtils.readResourceFile("compounding/patch-response-payment-not-found-404.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/payment/mark-as-noted")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-pending.sql")
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-noted should return 409 when not MAKE_NOTE")
    void markAsNoted_ReturnsConflict_WhenNotMakeNote() {
        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/payment/mark-as-noted")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("message")
                .isEqualTo("{\"status\": 409}");
    }

    @Test
    @Sql("/sql/compounding/insert-one-compounding-make-note.sql")
    @DisplayName("PATCH /compoundings/{id}/payment/mark-as-noted should return 401 when PIN is invalid")
    void markAsNoted_ReturnsUnauthorized_WhenPinIsInvalid() {
        var expected = fileUtils.readResourceFile("compounding/post-response-compounding-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .when()
                .patch(URL + "/00000000-0000-0000-0000-000000000070/payment/mark-as-noted")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }
}
