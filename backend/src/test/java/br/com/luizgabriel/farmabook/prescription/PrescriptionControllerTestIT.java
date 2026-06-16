package br.com.luizgabriel.farmabook.prescription;

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
class PrescriptionControllerTestIT extends AuthenticatedIntegrationConfig {

    private static final String URL = "/prescriptions";
    private static final UUID PRESCRIPTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final UUID ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000031");
    private static final UUID RECEIVED_ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000032");
    private static final UUID NONEXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-999999999999");

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    // --- POST /prescriptions ---

    @Test
    @Sql("/sql/prescription/insert-one-prescription-pending.sql")
    @DisplayName("POST /prescriptions should return 201 and persist prescription when successful")
    void save_ReturnsCreated_WhenSuccessful() {
        var request = fileUtils.readResourceFile("prescription/post-request-prescription.json");

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
                .body("customerName", Matchers.equalTo("Maria Silva"))
                .body("status", Matchers.equalTo("PENDING"))
                .body("observations", Matchers.equalTo("Receita de teste"))
                .body("items.size()", Matchers.equalTo(1));

        assertThat(prescriptionRepository.count()).isEqualTo(2);
        assertThat(prescriptionItemRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("POST /prescriptions should return 400 when required fields are missing")
    void save_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var request = fileUtils.readResourceFile("prescription/post-request-prescription-invalid-fields.json");
        var expected = fileUtils.readResourceFile("prescription/post-response-prescription-invalid-fields-400.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(400)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /prescriptions should return 401 when PIN is invalid")
    void save_ReturnsUnauthorized_WhenPinIsInvalid() {
        var request = fileUtils.readResourceFile("prescription/post-request-prescription.json");
        var expected = fileUtils.readResourceFile("prescription/post-response-prescription-unauthorized-401.json");

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
    @DisplayName("POST /prescriptions should return 404 when customer is not found")
    void save_ReturnsNotFound_WhenCustomerNotFound() {
        var request = "{\"customerId\": \"" + NONEXISTENT_ID + "\", \"items\": [{\"product\": \"Dipirona\", \"quantity\": 1, \"batch\": \"L01\", \"expiry\": \"12/2025\"}]}";
        var expected = fileUtils.readResourceFile("prescription/get-response-prescription-not-found-404.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }

    // --- GET /prescriptions ---

    @Test
    @Sql("/sql/prescription/insert-one-prescription-pending.sql")
    @DisplayName("GET /prescriptions should return 200 and list of prescriptions")
    void findAll_ReturnsOk_WhenSuccessful() {
        RestAssured.given()
                .when()
                .get(URL)
                .then()
                .log().all()
                .statusCode(200)
                .body("content.size()", Matchers.equalTo(1))
                .body("content[0].customerName", Matchers.equalTo("Maria Silva"))
                .body("content[0].status", Matchers.equalTo("PENDING"));
    }

    // --- GET /prescriptions/{id} ---

    @Test
    @Sql("/sql/prescription/insert-one-prescription-pending.sql")
    @DisplayName("GET /prescriptions/{id} should return 200 and prescription details when found")
    void findById_ReturnsOk_WhenFound() {
        RestAssured.given()
                .when()
                .get(URL + "/" + PRESCRIPTION_ID)
                .then()
                .log().all()
                .statusCode(200)
                .body("id", Matchers.equalTo(PRESCRIPTION_ID.toString()))
                .body("customerName", Matchers.equalTo("Maria Silva"))
                .body("status", Matchers.equalTo("PENDING"))
                .body("items.size()", Matchers.equalTo(1));
    }

    @Test
    @DisplayName("GET /prescriptions/{id} should return 404 when prescription is not found")
    void findById_ReturnsNotFound_WhenPrescriptionDoesNotExist() {
        var expected = fileUtils.readResourceFile("prescription/get-response-prescription-not-found-404.json");

        var body = RestAssured.given()
                .when()
                .get(URL + "/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }

    // --- PUT /prescriptions/{id} ---

    @Test
    @Sql("/sql/prescription/insert-one-prescription-pending.sql")
    @DisplayName("PUT /prescriptions/{id} should return 200 and update observations when successful")
    void update_ReturnsOk_WhenSuccessful() {
        var request = fileUtils.readResourceFile("prescription/put-request-prescription.json");

        RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + PRESCRIPTION_ID)
                .then()
                .log().all()
                .statusCode(200)
                .body("id", Matchers.equalTo(PRESCRIPTION_ID.toString()))
                .body("status", Matchers.equalTo("PENDING"));

        var prescription = prescriptionRepository.findById(PRESCRIPTION_ID).orElseThrow();
        assertThat(prescription.getObservations()).isEqualTo("Observação atualizada");
    }

    @Test
    @DisplayName("PUT /prescriptions/{id} should return 404 when prescription is not found")
    void update_ReturnsNotFound_WhenPrescriptionDoesNotExist() {
        var request = fileUtils.readResourceFile("prescription/put-request-prescription.json");
        var expected = fileUtils.readResourceFile("prescription/get-response-prescription-not-found-404.json");

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

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }

    @Test
    @Sql("/sql/prescription/insert-one-prescription-finished.sql")
    @DisplayName("PUT /prescriptions/{id} should return 409 when prescription is FINISHED")
    void update_ReturnsConflict_WhenPrescriptionIsFinished() {
        var request = fileUtils.readResourceFile("prescription/put-request-prescription.json");
        var expected = fileUtils.readResourceFile("prescription/put-response-prescription-finished-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + PRESCRIPTION_ID)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }

    // --- DELETE /prescriptions/{id} ---

    @Test
    @Sql("/sql/prescription/insert-one-prescription-pending.sql")
    @DisplayName("DELETE /prescriptions/{id} should return 204 and soft-delete when successful")
    void delete_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .delete(URL + "/" + PRESCRIPTION_ID)
                .then()
                .log().all()
                .statusCode(204);

        assertThat(prescriptionRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("DELETE /prescriptions/{id} should return 404 when prescription is not found")
    void delete_ReturnsNotFound_WhenPrescriptionDoesNotExist() {
        var expected = fileUtils.readResourceFile("prescription/get-response-prescription-not-found-404.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .delete(URL + "/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }

    @Test
    @Sql("/sql/prescription/insert-one-prescription-finished.sql")
    @DisplayName("DELETE /prescriptions/{id} should return 409 when prescription is FINISHED")
    void delete_ReturnsConflict_WhenPrescriptionIsFinished() {
        var expected = fileUtils.readResourceFile("prescription/delete-response-prescription-finished-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .delete(URL + "/" + PRESCRIPTION_ID)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }

    // --- POST /prescriptions/{id}/items ---

    @Test
    @Sql("/sql/prescription/insert-one-prescription-pending.sql")
    @DisplayName("POST /prescriptions/{id}/items should return 201 and persist the new item when successful")
    void addItem_ReturnsCreated_WhenSuccessful() {
        var request = fileUtils.readResourceFile("prescription/post-request-prescription-item.json");

        RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/" + PRESCRIPTION_ID + "/items")
                .then()
                .log().all()
                .statusCode(201)
                .body("id", Matchers.notNullValue())
                .body("product", Matchers.equalTo("Paracetamol 750mg"))
                .body("status", Matchers.equalTo("PENDING"));

        assertThat(prescriptionItemRepository.count()).isEqualTo(2);
    }

    @Test
    @Sql("/sql/prescription/insert-one-prescription-pending.sql")
    @DisplayName("POST /prescriptions/{id}/items should return 400 when item fields are invalid")
    void addItem_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var request = fileUtils.readResourceFile("prescription/post-request-prescription-item-invalid-fields.json");

        RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/" + PRESCRIPTION_ID + "/items")
                .then()
                .log().all()
                .statusCode(400);

        assertThat(prescriptionItemRepository.count()).isEqualTo(1);
    }

    @Test
    @Sql("/sql/prescription/insert-one-prescription-finished.sql")
    @DisplayName("POST /prescriptions/{id}/items should return 409 when prescription is FINISHED")
    void addItem_ReturnsConflict_WhenPrescriptionIsFinished() {
        var request = fileUtils.readResourceFile("prescription/post-request-prescription-item.json");
        var expected = fileUtils.readResourceFile("prescription/patch-response-prescription-item-finished-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/" + PRESCRIPTION_ID + "/items")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }

    // --- PUT /prescriptions/{id}/items/{itemId} ---

    @Test
    @Sql("/sql/prescription/insert-one-prescription-pending.sql")
    @DisplayName("PUT /prescriptions/{id}/items/{itemId} should return 200 and update the item when successful")
    void updateItem_ReturnsOk_WhenSuccessful() {
        var request = "{\"product\": \"Paracetamol 750mg\", \"quantity\": 3, \"batch\": \"LOTE-999\", \"expiry\": \"06/2026\"}";

        RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + PRESCRIPTION_ID + "/items/" + ITEM_ID)
                .then()
                .log().all()
                .statusCode(200)
                .body("product", Matchers.equalTo("Paracetamol 750mg"))
                .body("quantity", Matchers.equalTo(3));

        var item = prescriptionItemRepository.findById(ITEM_ID).orElseThrow();
        assertThat(item.getProduct()).isEqualTo("Paracetamol 750mg");
        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    @Sql("/sql/prescription/insert-one-prescription-with-received-item.sql")
    @DisplayName("PUT /prescriptions/{id}/items/{itemId} should return 409 when item is RECEIVED")
    void updateItem_ReturnsConflict_WhenItemIsReceived() {
        var request = "{\"product\": \"Paracetamol 750mg\", \"quantity\": 3, \"batch\": \"LOTE-999\", \"expiry\": \"06/2026\"}";
        var expected = fileUtils.readResourceFile("prescription/patch-response-prescription-item-already-received-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + PRESCRIPTION_ID + "/items/" + RECEIVED_ITEM_ID)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }

    // --- DELETE /prescriptions/{id}/items/{itemId} ---

    @Test
    @Sql("/sql/prescription/insert-one-prescription-pending.sql")
    @DisplayName("DELETE /prescriptions/{id}/items/{itemId} should return 204 and remove the item when successful")
    void deleteItem_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .delete(URL + "/" + PRESCRIPTION_ID + "/items/" + ITEM_ID)
                .then()
                .log().all()
                .statusCode(204);

        assertThat(prescriptionItemRepository.count()).isEqualTo(0);
    }

    @Test
    @Sql("/sql/prescription/insert-one-prescription-with-received-item.sql")
    @DisplayName("DELETE /prescriptions/{id}/items/{itemId} should return 409 when item is RECEIVED")
    void deleteItem_ReturnsConflict_WhenItemIsReceived() {
        var expected = fileUtils.readResourceFile("prescription/patch-response-prescription-item-already-received-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .delete(URL + "/" + PRESCRIPTION_ID + "/items/" + RECEIVED_ITEM_ID)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
        assertThat(prescriptionItemRepository.count()).isEqualTo(2);
    }

    // --- PATCH /prescriptions/{id}/items/{itemId}/mark-as-received ---

    @Test
    @Sql("/sql/prescription/insert-one-prescription-pending.sql")
    @DisplayName("PATCH mark-as-received should return 204 and set item to RECEIVED when successful")
    void markItemAsReceived_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/" + PRESCRIPTION_ID + "/items/" + ITEM_ID + "/mark-as-received")
                .then()
                .log().all()
                .statusCode(204);

        var item = prescriptionItemRepository.findById(ITEM_ID).orElseThrow();
        assertThat(item.getStatus()).isEqualTo(PrescriptionItemStatus.RECEIVED);
        assertThat(item.getReceivedAt()).isNotNull();

        var prescription = prescriptionRepository.findById(PRESCRIPTION_ID).orElseThrow();
        assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.FINISHED);
    }

    @Test
    @Sql("/sql/prescription/insert-one-prescription-with-received-item.sql")
    @DisplayName("PATCH mark-as-received should return 409 when item is already RECEIVED")
    void markItemAsReceived_ReturnsConflict_WhenItemAlreadyReceived() {
        var expected = fileUtils.readResourceFile("prescription/patch-response-prescription-item-already-received-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/" + PRESCRIPTION_ID + "/items/" + RECEIVED_ITEM_ID + "/mark-as-received")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }

    @Test
    @Sql("/sql/prescription/insert-one-prescription-finished.sql")
    @DisplayName("PATCH mark-as-received should return 409 when prescription is FINISHED")
    void markItemAsReceived_ReturnsConflict_WhenPrescriptionIsFinished() {
        var expected = fileUtils.readResourceFile("prescription/patch-response-prescription-item-finished-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/" + PRESCRIPTION_ID + "/items/" + ITEM_ID + "/mark-as-received")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }

    // --- PATCH /prescriptions/{id}/mark-as-received ---

    @Test
    @Sql("/sql/prescription/insert-one-prescription-pending.sql")
    @DisplayName("PATCH /prescriptions/{id}/mark-as-received should return 204 and transition all items to RECEIVED")
    void markAllAsReceived_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/" + PRESCRIPTION_ID + "/mark-as-received")
                .then()
                .log().all()
                .statusCode(204);

        var item = prescriptionItemRepository.findById(ITEM_ID).orElseThrow();
        assertThat(item.getStatus()).isEqualTo(PrescriptionItemStatus.RECEIVED);

        var prescription = prescriptionRepository.findById(PRESCRIPTION_ID).orElseThrow();
        assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.FINISHED);
    }

    @Test
    @DisplayName("PATCH /prescriptions/{id}/mark-as-received should return 404 when prescription is not found")
    void markAllAsReceived_ReturnsNotFound_WhenPrescriptionDoesNotExist() {
        var expected = fileUtils.readResourceFile("prescription/get-response-prescription-not-found-404.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/mark-as-received")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }

    @Test
    @Sql("/sql/prescription/insert-one-prescription-finished.sql")
    @DisplayName("PATCH /prescriptions/{id}/mark-as-received should return 409 when prescription is FINISHED")
    void markAllAsReceived_ReturnsConflict_WhenPrescriptionIsFinished() {
        var expected = fileUtils.readResourceFile("prescription/patch-response-prescription-item-finished-conflict-409.json");

        var body = RestAssured.given()
                .header(authPinHeader())
                .when()
                .patch(URL + "/" + PRESCRIPTION_ID + "/mark-as-received")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).whenIgnoringPaths("message").isEqualTo(expected);
    }
}
