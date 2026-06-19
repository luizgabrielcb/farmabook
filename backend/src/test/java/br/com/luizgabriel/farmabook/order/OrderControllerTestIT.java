package br.com.luizgabriel.farmabook.order;

import br.com.luizgabriel.farmabook.commons.FileUtils;
import br.com.luizgabriel.farmabook.config.IntegrationTestConfig;
import br.com.luizgabriel.farmabook.notification.NotificationRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(value = "/sql/clean-database.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class OrderControllerTestIT extends IntegrationTestConfig {

    private static final String URL = "/orders";
    private static final String AUTH_PIN = "1234";
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID NONEXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-999999999999");
    private static final UUID DISTRIBUTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @Sql("/sql/order/insert-customer.sql")
    @DisplayName("POST /orders should return 201 and persist order when successful")
    void save_ReturnsCreated_WhenSuccessful() {
        var request = fileUtils.readResourceFile("order/post-request-order.json");

        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
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
                .body("items.size()", Matchers.equalTo(1));

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderItemRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /orders should return 400 when items list is empty")
    void save_ReturnsBadRequest_WhenItemsListIsEmpty() {
        var request = fileUtils.readResourceFile("order/post-request-order-invalid-fields.json");
        var expected = fileUtils.readResourceFile("order/post-response-order-invalid-fields-400.json");

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
    @DisplayName("POST /orders should return 401 when PIN is invalid")
    void save_ReturnsUnauthorized_WhenPinIsInvalid() {
        var request = fileUtils.readResourceFile("order/post-request-order.json");
        var expected = fileUtils.readResourceFile("order/post-response-order-unauthorized-401.json");

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
    @DisplayName("POST /orders should return 404 when customer is not found")
    void save_ReturnsNotFound_WhenCustomerNotFound() {
        var expected = fileUtils.readResourceFile("order/post-response-order-customer-not-found-404.json");
        var request = "{\"customerId\": \"" + NONEXISTENT_ID + "\", \"items\": [{\"product\": \"Dipirona\", \"category\": \"MEDICAMENTOS\", \"quantity\": 1}]}";

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("GET /orders should return 200 with a page of orders when successful")
    void findAll_ReturnsPageOfOrders_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("order/get-response-order-list-200.json");

        var body = RestAssured.given()
                .when()
                .get(URL)
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("content[*].id", "content[*].customerId", "content[*].createdById",
                        "content[*].createdAt", "content[*].updatedAt",
                        "content[*].items[*].id", "content[*].items[*].createdAt", "content[*].items[*].updatedAt",
                        "pageable", "last", "first", "size", "number", "sort", "numberOfElements", "empty")
                .isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("GET /orders/{id} should return 200 with order when found")
    void findById_ReturnsOrder_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("order/get-response-order-by-id-200.json");

        var body = RestAssured.given()
                .when()
                .get(URL + "/" + ORDER_ID)
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "customerId", "createdById", "createdAt", "updatedAt",
                        "items[*].id", "items[*].createdAt", "items[*].updatedAt")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /orders/{id} should return 404 when order is not found")
    void findById_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

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
    @Sql("/sql/order/insert-one-order-pending-two-customers.sql")
    @DisplayName("PUT /orders/{id} should return 200 and update customer when successful")
    void update_ReturnsOk_WhenSuccessful() {
        var request = fileUtils.readResourceFile("order/put-request-order.json");
        var expected = fileUtils.readResourceFile("order/put-response-order-200.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + ORDER_ID)
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "customerId", "updatedAt")
                .isEqualTo(expected);

        var updated = orderRepository.findById(ORDER_ID).orElseThrow();
        assertThat(updated.getCustomerName()).isEqualTo("José Santos");
    }

    @Test
    @DisplayName("PUT /orders/{id} should return 404 when order is not found")
    void update_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body("{\"customerId\": \"" + NONEXISTENT_ID + "\"}")
                .when()
                .put(URL + "/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-delivered.sql")
    @DisplayName("PUT /orders/{id} should return 409 when order is DELIVERED")
    void update_ReturnsConflict_WhenOrderIsDelivered() {
        var expected = fileUtils.readResourceFile("order/put-response-order-delivered-conflict-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body("{\"customerId\": \"00000000-0000-0000-0000-000000000001\"}")
                .when()
                .put(URL + "/" + ORDER_ID)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("DELETE /orders/{id} should return 204 and soft-delete order when successful")
    void delete_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .delete(URL + "/" + ORDER_ID)
                .then()
                .log().all()
                .statusCode(204);

        assertThat(orderRepository.findById(ORDER_ID)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /orders/{id} should return 404 when order is not found")
    void delete_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

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
    @Sql("/sql/order/insert-one-order-delivered.sql")
    @DisplayName("DELETE /orders/{id} should return 409 when order is DELIVERED")
    void delete_ReturnsConflict_WhenOrderIsDelivered() {
        var expected = fileUtils.readResourceFile("order/delete-response-order-delivered-conflict-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .delete(URL + "/" + ORDER_ID)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-with-delivered-item.sql")
    @DisplayName("DELETE /orders/{id} should return 409 when order has DELIVERED items")
    void delete_ReturnsConflict_WhenOrderHasDeliveredItems() {
        var expected = fileUtils.readResourceFile("order/delete-response-order-delivered-items-conflict-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .delete(URL + "/" + ORDER_ID)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql({"/sql/order/insert-one-order-pending.sql", "/sql/distributor/insert-one-distributor.sql"})
    @DisplayName("PATCH /orders/{id}/mark-as-ordered should return 204 and transition all items to ORDERED when successful")
    void markAllAsOrdered_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body("{\"distributorId\": \"" + DISTRIBUTOR_ID + "\"}")
                .when()
                .patch(URL + "/" + ORDER_ID + "/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(204);

        var order = orderRepository.findWithItemsById(ORDER_ID).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
        assertThat(order.getItems()).allMatch(i -> i.getStatus() == OrderItemStatus.ORDERED);
        assertThat(order.getItems()).allMatch(i -> "User Teste".equals(i.getOrderedByName()));
        assertThat(order.getItems()).allMatch(i -> i.getOrderedAt() != null);
    }

    @Test
    @Sql("/sql/distributor/insert-one-distributor.sql")
    @DisplayName("PATCH /orders/{id}/mark-as-ordered should return 404 when order is not found")
    void markAllAsOrdered_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body("{\"distributorId\": \"" + DISTRIBUTOR_ID + "\"}")
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-ordered.sql")
    @DisplayName("PATCH /orders/{id}/mark-as-received should return 204, transition items to RECEIVED and generate notification when successful")
    void markAllAsReceived_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/mark-as-received")
                .then()
                .log().all()
                .statusCode(204);

        var order = orderRepository.findWithItemsById(ORDER_ID).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(order.getNotifiedAt()).isNotNull();
        assertThat(order.getItems()).allMatch(i -> i.getStatus() == OrderItemStatus.RECEIVED);
        assertThat(order.getItems()).allMatch(i -> "User Teste".equals(i.getReceivedByName()));

        var notifications = notificationRepository.findAllByOrderId(ORDER_ID, Pageable.unpaged());
        assertThat(notifications.getTotalElements()).isEqualTo(1);
        assertThat(notifications.getContent().getFirst().getCustomerPhone()).isEqualTo("5511999999999");
    }

    @Test
    @DisplayName("PATCH /orders/{id}/mark-as-received should return 404 when order is not found")
    void markAllAsReceived_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/mark-as-received")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-received.sql")
    @DisplayName("PATCH /orders/{id}/mark-as-delivered should return 204 and transition all items to DELIVERED when successful")
    void markAllAsDelivered_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/mark-as-delivered")
                .then()
                .log().all()
                .statusCode(204);

        var order = orderRepository.findWithItemsById(ORDER_ID).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getItems()).allMatch(i -> i.getStatus() == OrderItemStatus.DELIVERED);
        assertThat(order.getItems()).allMatch(i -> "User Teste".equals(i.getDeliveredByName()));
        assertThat(order.getItems()).allMatch(i -> i.getDeliveredAt() != null);
    }

    @Test
    @DisplayName("PATCH /orders/{id}/mark-as-delivered should return 404 when order is not found")
    void markAllAsDelivered_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/mark-as-delivered")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("POST /orders/{id}/items should return 201 and add item when successful")
    void addItem_ReturnsCreated_WhenSuccessful() {
        var request = fileUtils.readResourceFile("order/post-request-order-item.json");

        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/" + ORDER_ID + "/items")
                .then()
                .log().all()
                .statusCode(201)
                .body("id", Matchers.notNullValue())
                .body("product", Matchers.equalTo("Paracetamol 750mg"))
                .body("status", Matchers.equalTo("PENDING"));

        assertThat(orderItemRepository.count()).isEqualTo(2);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("POST /orders/{id}/items should return 400 when fields are invalid")
    void addItem_ReturnsBadRequest_WhenFieldsAreInvalid() {
        var request = fileUtils.readResourceFile("order/post-request-order-item-invalid-fields.json");
        var expected = fileUtils.readResourceFile("order/post-response-order-item-invalid-fields-400.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/" + ORDER_ID + "/items")
                .then()
                .log().all()
                .statusCode(400)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /orders/{id}/items should return 404 when order is not found")
    void addItem_ReturnsNotFound_WhenOrderNotFound() {
        var request = fileUtils.readResourceFile("order/post-request-order-item.json");
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

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
    @Sql("/sql/order/insert-one-order-delivered.sql")
    @DisplayName("POST /orders/{id}/items should return 409 when order is DELIVERED")
    void addItem_ReturnsConflict_WhenOrderIsDelivered() {
        var request = fileUtils.readResourceFile("order/post-request-order-item.json");
        var expected = fileUtils.readResourceFile("order/put-response-order-delivered-conflict-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(URL + "/" + ORDER_ID + "/items")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("PUT /orders/{id}/items/{itemId} should return 200 and update item when successful")
    void updateItem_ReturnsOk_WhenSuccessful() {
        var request = fileUtils.readResourceFile("order/put-request-order-item.json");
        var expected = fileUtils.readResourceFile("order/put-response-order-item-200.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + ORDER_ID + "/items/" + ITEM_ID)
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("id", "createdAt", "updatedAt")
                .isEqualTo(expected);

        var item = orderItemRepository.findById(ITEM_ID).orElseThrow();
        assertThat(item.getProduct()).isEqualTo("Paracetamol 750mg");
    }

    @Test
    @DisplayName("PUT /orders/{id}/items/{itemId} should return 404 when order is not found")
    void updateItem_ReturnsNotFound_WhenOrderNotFound() {
        var request = fileUtils.readResourceFile("order/put-request-order-item.json");
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + NONEXISTENT_ID + "/items/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("PUT /orders/{id}/items/{itemId} should return 404 when item is not found in the order")
    void updateItem_ReturnsNotFound_WhenItemNotFound() {
        var request = fileUtils.readResourceFile("order/put-request-order-item.json");
        var expected = fileUtils.readResourceFile("order/get-response-order-item-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + ORDER_ID + "/items/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-delivered.sql")
    @DisplayName("PUT /orders/{id}/items/{itemId} should return 409 when item is DELIVERED")
    void updateItem_ReturnsConflict_WhenItemIsDelivered() {
        var request = fileUtils.readResourceFile("order/put-request-order-item.json");
        var expected = fileUtils.readResourceFile("order/put-response-order-item-delivered-conflict-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put(URL + "/" + ORDER_ID + "/items/" + ITEM_ID)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("DELETE /orders/{id}/items/{itemId} should return 204 and soft-delete item when successful")
    void deleteItem_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .delete(URL + "/" + ORDER_ID + "/items/" + ITEM_ID)
                .then()
                .log().all()
                .statusCode(204);

        assertThat(orderItemRepository.findById(ITEM_ID)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /orders/{id}/items/{itemId} should return 404 when order is not found")
    void deleteItem_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .delete(URL + "/" + NONEXISTENT_ID + "/items/" + NONEXISTENT_ID)
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-delivered.sql")
    @DisplayName("DELETE /orders/{id}/items/{itemId} should return 409 when item is DELIVERED")
    void deleteItem_ReturnsConflict_WhenItemIsDelivered() {
        var expected = fileUtils.readResourceFile("order/put-response-order-item-delivered-conflict-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .delete(URL + "/" + ORDER_ID + "/items/" + ITEM_ID)
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql({"/sql/order/insert-one-order-pending.sql", "/sql/distributor/insert-one-distributor.sql"})
    @DisplayName("PATCH /orders/{id}/items/{itemId}/mark-as-ordered should return 204 and transition item when successful")
    void markItemAsOrdered_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body("{\"distributorId\": \"" + DISTRIBUTOR_ID + "\"}")
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(204);

        var item = orderItemRepository.findById(ITEM_ID).orElseThrow();
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.ORDERED);
        assertThat(item.getOrderedByName()).isEqualTo("User Teste");
        assertThat(item.getOrderedAt()).isNotNull();
    }

    @Test
    @Sql("/sql/distributor/insert-one-distributor.sql")
    @DisplayName("PATCH /orders/{id}/items/{itemId}/mark-as-ordered should return 404 when order is not found")
    void markItemAsOrdered_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body("{\"distributorId\": \"" + DISTRIBUTOR_ID + "\"}")
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/items/" + NONEXISTENT_ID + "/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql({"/sql/order/insert-one-order-delivered.sql", "/sql/distributor/insert-one-distributor.sql"})
    @DisplayName("PATCH /orders/{id}/items/{itemId}/mark-as-ordered should return 409 when item is DELIVERED")
    void markItemAsOrdered_ReturnsConflict_WhenItemIsDelivered() {
        var expected = fileUtils.readResourceFile("order/patch-response-order-item-mark-ordered-conflict-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .contentType(ContentType.JSON)
                .body("{\"distributorId\": \"" + DISTRIBUTOR_ID + "\"}")
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/mark-as-ordered")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-ordered.sql")
    @DisplayName("PATCH /orders/{id}/items/{itemId}/mark-as-received should return 204 and transition item when successful")
    void markItemAsReceived_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/mark-as-received")
                .then()
                .log().all()
                .statusCode(204);

        var item = orderItemRepository.findById(ITEM_ID).orElseThrow();
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.RECEIVED);
        assertThat(item.getReceivedByName()).isEqualTo("User Teste");
        assertThat(item.getReceivedAt()).isNotNull();
    }

    @Test
    @DisplayName("PATCH /orders/{id}/items/{itemId}/mark-as-received should return 404 when order is not found")
    void markItemAsReceived_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/items/" + NONEXISTENT_ID + "/mark-as-received")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("PATCH /orders/{id}/items/{itemId}/mark-as-received should return 409 when item is PENDING")
    void markItemAsReceived_ReturnsConflict_WhenItemIsPending() {
        var expected = fileUtils.readResourceFile("order/patch-response-order-item-mark-received-conflict-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/mark-as-received")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-received.sql")
    @DisplayName("PATCH /orders/{id}/items/{itemId}/mark-as-delivered should return 204 and transition item when successful")
    void markItemAsDelivered_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/mark-as-delivered")
                .then()
                .log().all()
                .statusCode(204);

        var item = orderItemRepository.findById(ITEM_ID).orElseThrow();
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.DELIVERED);
        assertThat(item.getDeliveredByName()).isEqualTo("User Teste");
        assertThat(item.getDeliveredAt()).isNotNull();
    }

    @Test
    @DisplayName("PATCH /orders/{id}/items/{itemId}/mark-as-delivered should return 404 when order is not found")
    void markItemAsDelivered_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/items/" + NONEXISTENT_ID + "/mark-as-delivered")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-ordered.sql")
    @DisplayName("PATCH /orders/{id}/items/{itemId}/mark-as-delivered should return 409 when item is ORDERED")
    void markItemAsDelivered_ReturnsConflict_WhenItemIsOrdered() {
        var expected = fileUtils.readResourceFile("order/patch-response-order-item-mark-delivered-conflict-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/mark-as-delivered")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("PATCH .../payment/mark-as-paid should return 204 and set item payment to PAID when successful")
    void markItemPaymentAsPaid_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/payment/mark-as-paid")
                .then()
                .log().all()
                .statusCode(204);

        var item = orderItemRepository.findById(ITEM_ID).orElseThrow();
        assertThat(item.getPaymentStatus()).isEqualTo(OrderPaymentStatus.PAID);
        assertThat(item.getPaymentChangedByName()).isEqualTo("User Teste");
        assertThat(item.getPaymentChangedAt()).isNotNull();
    }

    @Test
    @Sql("/sql/order/insert-one-order-payment-noted.sql")
    @DisplayName("PATCH .../payment/mark-as-paid should return 409 when payment is NOTED")
    void markItemPaymentAsPaid_ReturnsConflict_WhenNoted() {
        var expected = fileUtils.readResourceFile("order/patch-response-payment-paid-conflict-noted-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/payment/mark-as-paid")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @DisplayName("PATCH .../payment/mark-as-paid should return 404 when order is not found")
    void markItemPaymentAsPaid_ReturnsNotFound_WhenOrderNotFound() {
        var expected = fileUtils.readResourceFile("order/get-response-order-not-found-404.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + NONEXISTENT_ID + "/items/" + ITEM_ID + "/payment/mark-as-paid")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("PATCH .../payment/mark-as-paid should return 401 when PIN is invalid")
    void markItemPaymentAsPaid_ReturnsUnauthorized_WhenPinIsInvalid() {
        var expected = fileUtils.readResourceFile("order/post-response-order-unauthorized-401.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", "9999")
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/payment/mark-as-paid")
                .then()
                .log().all()
                .statusCode(401)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("PATCH .../payment/mark-as-make-note should return 204 and set item payment to MAKE_NOTE when successful")
    void markItemPaymentAsMakeNote_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/payment/mark-as-make-note")
                .then()
                .log().all()
                .statusCode(204);

        var item = orderItemRepository.findById(ITEM_ID).orElseThrow();
        assertThat(item.getPaymentStatus()).isEqualTo(OrderPaymentStatus.MAKE_NOTE);
        assertThat(item.getPaymentChangedByName()).isEqualTo("User Teste");
    }

    @Test
    @Sql("/sql/order/insert-one-order-payment-make-note.sql")
    @DisplayName("PATCH .../payment/mark-as-noted should return 204 and set item payment to NOTED when current is MAKE_NOTE")
    void markItemPaymentAsNoted_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/payment/mark-as-noted")
                .then()
                .log().all()
                .statusCode(204);

        var item = orderItemRepository.findById(ITEM_ID).orElseThrow();
        assertThat(item.getPaymentStatus()).isEqualTo(OrderPaymentStatus.NOTED);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("PATCH .../payment/mark-as-noted should return 409 when current is not MAKE_NOTE")
    void markItemPaymentAsNoted_ReturnsConflict_WhenNotMakeNote() {
        var expected = fileUtils.readResourceFile("order/patch-response-payment-noted-conflict-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/payment/mark-as-noted")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }

    @Test
    @Sql("/sql/order/insert-one-order-payment-make-note.sql")
    @DisplayName("PATCH .../payment/mark-as-to-pay should return 204 and revert item payment to TO_PAY when current is MAKE_NOTE")
    void markItemPaymentAsToPay_ReturnsNoContent_WhenSuccessful() {
        RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/payment/mark-as-to-pay")
                .then()
                .log().all()
                .statusCode(204);

        var item = orderItemRepository.findById(ITEM_ID).orElseThrow();
        assertThat(item.getPaymentStatus()).isEqualTo(OrderPaymentStatus.TO_PAY);
    }

    @Test
    @Sql("/sql/order/insert-one-order-pending.sql")
    @DisplayName("PATCH .../payment/mark-as-to-pay should return 409 when current is not MAKE_NOTE")
    void markItemPaymentAsToPay_ReturnsConflict_WhenNotMakeNote() {
        var expected = fileUtils.readResourceFile("order/patch-response-payment-to-pay-conflict-409.json");

        var body = RestAssured.given()
                .header("X-Auth-Pin", AUTH_PIN)
                .when()
                .patch(URL + "/" + ORDER_ID + "/items/" + ITEM_ID + "/payment/mark-as-to-pay")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }
}
