package br.com.luizgabriel.farmabook.shortage;

import br.com.luizgabriel.farmabook.commons.FileUtils;
import br.com.luizgabriel.farmabook.config.IntegrationTestConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(value = "/sql/clean-database.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class ShortageOrderControllerTestIT extends IntegrationTestConfig {

    private static final String URL = "/shortage-orders";
    private static final String AUTH_PIN = "1234";
    private static final String SHORTAGE_ORDER_ID = "00000000-0000-0000-0000-000000000060";
    private static final UUID NONEXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-999999999999");

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private ShortageOrderRepository shortageOrderRepository;

    @Autowired
    private ShortageRepository shortageRepository;

    // --- POST ---

    @Test
    @Sql("/sql/distributor/insert-one-distributor.sql")
    @DisplayName("POST /shortage-orders should return 201 and persist the order and its shortages when successful")
    void save_ReturnsCreated_WhenSuccessful() {
        var request = fileUtils.readResourceFile("shortage-order/post-request-shortage-order.json");

        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(201)
                .body("id", org.hamcrest.Matchers.notNullValue());

        var saved = shortageOrderRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStatus()).isEqualTo(ShortageOrderStatus.PENDING);
        assertThat(saved.get(0).getDistributorName()).isEqualTo("Distribuidora Teste");
        assertThat(saved.get(0).getCreatedByName()).isEqualTo("User Teste");

        var shortages = shortageRepository.findAllByShortageOrderId(saved.get(0).getId());
        assertThat(shortages).hasSize(1);
        assertThat(shortages.get(0).getProduct()).isEqualTo("Dipirona 500mg");
        assertThat(shortages.get(0).getStatus()).isEqualTo(ShortageStatus.PENDING);
    }

    @Test
    @DisplayName("POST /shortage-orders should return 400 when required fields are invalid")
    void save_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var expected = fileUtils.readResourceFile("shortage-order/post-response-shortage-order-invalid-fields-400.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body("{\"shortageType\": null, \"distributorId\": null, \"items\": []}")
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(400)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /shortage-orders should return 401 when PIN is invalid")
    void save_ReturnsUnauthorized_WhenPinIsInvalid() {
        var request = fileUtils.readResourceFile("shortage-order/post-request-shortage-order.json");
        var expected = fileUtils.readResourceFile("shortage-order/post-response-shortage-order-unauthorized-401.json");

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

    // --- POST items ---

    @Test
    @Sql("/sql/shortage-order/insert-one-shortage-order.sql")
    @DisplayName("POST /shortage-orders/{id}/items should return 201 and add a shortage to the order when successful")
    void addItem_ReturnsCreated_WhenSuccessful() {
        var request = fileUtils.readResourceFile("shortage-order/post-request-shortage-order-item.json");

        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/" + SHORTAGE_ORDER_ID + "/items")
                .then()
                .log().all()
                .statusCode(201);

        var shortages = shortageRepository.findAllByShortageOrderId(UUID.fromString(SHORTAGE_ORDER_ID));
        assertThat(shortages).hasSize(2);
        assertThat(shortages).anyMatch(s -> s.getProduct().equals("Amoxicilina 875mg")
                && s.getStatus() == ShortageStatus.PENDING);
    }

    @Test
    @DisplayName("POST /shortage-orders/{id}/items should return 404 when order is not found")
    void addItem_ReturnsNotFound_WhenOrderNotFound() {
        var request = fileUtils.readResourceFile("shortage-order/post-request-shortage-order-item.json");
        var expected = fileUtils.readResourceFile("shortage-order/put-response-shortage-order-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/" + NONEXISTENT_ID + "/items")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /shortage-orders/{id}/items should return 401 when PIN is invalid")
    void addItem_ReturnsUnauthorized_WhenPinIsInvalid() {
        var request = fileUtils.readResourceFile("shortage-order/post-request-shortage-order-item.json");
        var expected = fileUtils.readResourceFile("shortage-order/post-response-shortage-order-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/" + SHORTAGE_ORDER_ID + "/items")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/shortage-order/insert-one-shortage-order-ordered.sql")
    @DisplayName("POST /shortage-orders/{id}/items should return 409 when order is already ORDERED")
    void addItem_ReturnsConflict_WhenOrderIsAlreadyOrdered() {
        var request = fileUtils.readResourceFile("shortage-order/post-request-shortage-order-item.json");
        var expected = fileUtils.readResourceFile("shortage-order/put-response-shortage-order-already-ordered-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/" + SHORTAGE_ORDER_ID + "/items")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    // --- GET (list) ---

    @Test
    @Sql("/sql/shortage-order/insert-one-shortage-order.sql")
    @DisplayName("GET /shortage-orders should return 200 with a page filtered by shortage type when successful")
    void findAll_ReturnsPageOfShortageOrders_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("shortage-order/get-response-shortage-order-list-200.json");

        var body = RestAssured.given()
                .when()
                .get(URL + "?shortageType=WANIA")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo(expected);
    }

    // --- GET (by id) ---

    @Test
    @Sql("/sql/shortage-order/insert-one-shortage-order.sql")
    @DisplayName("GET /shortage-orders/{id} should return 200 with the order and its shortages when found")
    void findById_ReturnsShortageOrder_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("shortage-order/get-response-shortage-order-by-id-200.json");

        var body = RestAssured.given()
                .when()
                .get(URL + "/" + SHORTAGE_ORDER_ID)
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /shortage-orders/{id} should return 404 when order is not found")
    void findById_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("shortage-order/get-response-shortage-order-not-found-404.json");

        var body = RestAssured.given()
                .when()
                .get(URL + "/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    // --- PUT ---

    @Test
    @Sql("/sql/shortage-order/insert-one-shortage-order.sql")
    @DisplayName("PUT /shortage-orders/{id} should return 200 and update the order when successful")
    void update_ReturnsOk_WhenSuccessful() {
        var request = fileUtils.readResourceFile("shortage-order/put-request-shortage-order.json");
        var expected = fileUtils.readResourceFile("shortage-order/put-response-shortage-order-200.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + SHORTAGE_ORDER_ID)
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo(expected);

        var updated = shortageOrderRepository.findById(UUID.fromString(SHORTAGE_ORDER_ID)).orElseThrow();
        assertThat(updated.getObservations()).isEqualTo("Observações atualizadas");
    }

    @Test
    @DisplayName("PUT /shortage-orders/{id} should return 404 when order is not found")
    void update_ReturnsNotFound_WhenOrderNotFound() {
        var request = fileUtils.readResourceFile("shortage-order/put-request-shortage-order.json");
        var expected = fileUtils.readResourceFile("shortage-order/put-response-shortage-order-not-found-404.json");

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
    @DisplayName("PUT /shortage-orders/{id} should return 401 when PIN is invalid")
    void update_ReturnsUnauthorized_WhenPinIsInvalid() {
        var request = fileUtils.readResourceFile("shortage-order/put-request-shortage-order.json");
        var expected = fileUtils.readResourceFile("shortage-order/post-response-shortage-order-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + SHORTAGE_ORDER_ID)
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/shortage-order/insert-one-shortage-order-ordered.sql")
    @DisplayName("PUT /shortage-orders/{id} should return 409 when order is already ORDERED")
    void update_ReturnsConflict_WhenOrderIsAlreadyOrdered() {
        var request = fileUtils.readResourceFile("shortage-order/put-request-shortage-order.json");
        var expected = fileUtils.readResourceFile("shortage-order/put-response-shortage-order-already-ordered-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + SHORTAGE_ORDER_ID)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    // --- DELETE ---

    @Test
    @Sql("/sql/shortage-order/insert-one-shortage-order.sql")
    @DisplayName("DELETE /shortage-orders/{id} should return 204 and soft-delete the order when successful")
    void delete_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .delete(URL + "/" + SHORTAGE_ORDER_ID)
                .then()
                .log().all()
                .statusCode(204);

        assertThat(shortageOrderRepository.findById(UUID.fromString(SHORTAGE_ORDER_ID))).isEmpty();
    }

    @Test
    @DisplayName("DELETE /shortage-orders/{id} should return 404 when order is not found")
    void delete_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("shortage-order/delete-response-shortage-order-not-found-404.json");

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
    @DisplayName("DELETE /shortage-orders/{id} should return 401 when PIN is invalid")
    void delete_ReturnsUnauthorized_WhenPinIsInvalid() {
        var expected = fileUtils.readResourceFile("shortage-order/post-response-shortage-order-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .when()
                .delete(URL + "/" + SHORTAGE_ORDER_ID)
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/shortage-order/insert-one-shortage-order-ordered.sql")
    @DisplayName("DELETE /shortage-orders/{id} should return 409 when order is already ORDERED")
    void delete_ReturnsConflict_WhenOrderIsAlreadyOrdered() {
        var expected = fileUtils.readResourceFile("shortage-order/delete-response-shortage-order-already-ordered-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .delete(URL + "/" + SHORTAGE_ORDER_ID)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    // --- PATCH mark-as-ordered ---

    @Test
    @Sql("/sql/shortage-order/insert-one-shortage-order.sql")
    @DisplayName("PATCH /shortage-orders/{id}/mark-as-ordered should return 204 and transition the order and its shortages")
    void markAsOrdered_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + SHORTAGE_ORDER_ID + "/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(204);

        var updated = shortageOrderRepository.findById(UUID.fromString(SHORTAGE_ORDER_ID)).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ShortageOrderStatus.ORDERED);
        assertThat(updated.getOrderedByName()).isEqualTo("User Teste");
        assertThat(updated.getOrderedAt()).isNotNull();

        var shortages = shortageRepository.findAllByShortageOrderId(updated.getId());
        assertThat(shortages).allMatch(s -> s.getStatus() == ShortageStatus.ORDERED);
    }

    @Test
    @DisplayName("PATCH /shortage-orders/{id}/mark-as-ordered should return 404 when order is not found")
    void markAsOrdered_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("shortage-order/patch-response-shortage-order-not-found-404.json");

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
    @Sql("/sql/shortage-order/insert-one-shortage-order-ordered.sql")
    @DisplayName("PATCH /shortage-orders/{id}/mark-as-ordered should return 409 when order is already ORDERED")
    void markAsOrdered_ReturnsConflict_WhenOrderIsAlreadyOrdered() {
        var expected = fileUtils.readResourceFile("shortage-order/patch-response-shortage-order-already-ordered-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + SHORTAGE_ORDER_ID + "/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }
}
