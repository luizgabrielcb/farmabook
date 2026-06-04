package br.com.luizgabriel.farmaorder.customer;

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

@Sql(value = "/sql/customer/delete-customers.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class CustomerControllerTestIT extends IntegrationTestConfig {

    private static final String URL = "/customers";
    private static final UUID NONEXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-999999999999");

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("POST /customers should return 201 and persist customer when successful")
    void save_ReturnsCreated_WhenSuccessful() {
        var request = fileUtils.readResourceFile("customer/post-request-customer.json");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(201)
                .body("id", Matchers.notNullValue());

        var saved = customerRepository.findByNameIgnoreCase("Carlos Comprador");
        assertThat(saved).isPresent();
        assertThat(saved.get().getPhoneNumber()).isEqualTo("11777777777");
    }

    @Test
    @DisplayName("POST /customers should return 400 when required fields are invalid")
    void save_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var request = fileUtils.readResourceFile("customer/post-request-customer-invalid-fields.json");
        var expected = fileUtils.readResourceFile("customer/post-response-customer-invalid-fields-400.json");

        var body = RestAssured.given()
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
    @Sql("/sql/customer/insert-one-customer.sql")
    @DisplayName("POST /customers should return 409 when name is already in use")
    void save_ReturnsConflict_WhenNameAlreadyInUse() {
        var request = fileUtils.readResourceFile("customer/post-request-customer-duplicate-name.json");
        var expected = fileUtils.readResourceFile("customer/post-response-customer-duplicate-name-409.json");

        var body = RestAssured.given()
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
    @Sql("/sql/customer/insert-one-customer.sql")
    @DisplayName("POST /customers should return 409 when phone number is already in use")
    void save_ReturnsConflict_WhenPhoneNumberAlreadyInUse() {
        var request = fileUtils.readResourceFile("customer/post-request-customer-duplicate-phone.json");
        var expected = fileUtils.readResourceFile("customer/post-response-customer-duplicate-phone-409.json");

        var body = RestAssured.given()
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
    @Sql("/sql/customer/insert-two-customers.sql")
    @DisplayName("GET /customers should return 200 with a page of customers when successful")
    void findAll_ReturnsPageOfCustomers_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("customer/get-response-customer-list-200.json");

        var body = RestAssured.given()
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
    @Sql("/sql/customer/insert-one-customer.sql")
    @DisplayName("GET /customers/{id} should return 200 with customer when found")
    void findById_ReturnsCustomer_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("customer/get-response-customer-by-id-200.json");
        var customer = customerRepository.findByNameIgnoreCase("Maria Silva").orElseThrow();

        var body = RestAssured.given()
                .when()
                .get(URL + "/" + customer.getId())
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /customers/{id} should return 404 when customer is not found")
    void findById_ReturnsNotFound_WhenCustomerNotFound() {
        var expected = fileUtils.readResourceFile("customer/get-response-customer-not-found-404.json");

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
    @Sql("/sql/customer/insert-one-customer.sql")
    @DisplayName("PUT /customers/{id} should return 200 and update customer when successful")
    void update_ReturnsOk_WhenSuccessful() {
        var request = fileUtils.readResourceFile("customer/put-request-customer.json");
        var expected = fileUtils.readResourceFile("customer/put-response-customer-200.json");
        var customer = customerRepository.findByNameIgnoreCase("Maria Silva").orElseThrow();

        var body = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + customer.getId())
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "updatedAt")
                .isEqualTo(expected);

        var updated = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Maria Souza");
        assertThat(updated.getPhoneNumber()).isEqualTo("11555555555");
    }

    @Test
    @Sql("/sql/customer/insert-one-customer.sql")
    @DisplayName("PUT /customers/{id} should return 400 when required fields are invalid")
    void update_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var expected = fileUtils.readResourceFile("customer/post-response-customer-invalid-fields-400.json");
        var customer = customerRepository.findByNameIgnoreCase("Maria Silva").orElseThrow();

        var body = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"\"}")
                .when()
                .put(URL + "/" + customer.getId())
                .then()
                .log().all()
                .statusCode(400)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("PUT /customers/{id} should return 404 when customer is not found")
    void update_ReturnsNotFound_WhenCustomerNotFound() {
        var request = fileUtils.readResourceFile("customer/put-request-customer.json");
        var expected = fileUtils.readResourceFile("customer/put-response-customer-not-found-404.json");

        var body = RestAssured.given()
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
    @Sql("/sql/customer/insert-two-customers.sql")
    @DisplayName("PUT /customers/{id} should return 409 when name is already in use by another customer")
    void update_ReturnsConflict_WhenNameAlreadyInUseByAnotherCustomer() {
        var expected = fileUtils.readResourceFile("customer/put-response-customer-conflict-409.json");
        var customer = customerRepository.findByNameIgnoreCase("Maria Silva").orElseThrow();

        var body = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"José Santos\", \"phoneNumber\": \"11000000000\"}")
                .when()
                .put(URL + "/" + customer.getId())
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/customer/insert-one-customer.sql")
    @DisplayName("DELETE /customers/{id} should return 204 and soft-delete customer when successful")
    void delete_ReturnsNoContent_WhenSuccessful() {
        var customer = customerRepository.findByNameIgnoreCase("Maria Silva").orElseThrow();

        RestAssured.given()
                .when()
                .delete(URL + "/" + customer.getId())
                .then()
                .log().all()
                .statusCode(204);

        assertThat(customerRepository.findById(customer.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /customers/{id} should return 404 when customer is not found")
    void delete_ReturnsNotFound_WhenCustomerNotFound() {
        var expected = fileUtils.readResourceFile("customer/delete-response-customer-not-found-404.json");

        var body = RestAssured.given()
                .when()
                .delete(URL + "/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }
}
