package br.com.luizgabriel.farmaorder.notification;

import br.com.luizgabriel.farmaorder.commons.FileUtils;
import br.com.luizgabriel.farmaorder.config.IntegrationTestConfig;
import io.restassured.RestAssured;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(value = "/sql/notification/delete-notifications.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class NotificationControllerTestIT extends IntegrationTestConfig {

    private static final String ORDER_ID = "00000000-0000-0000-0000-000000000010";
    private static final String NOTIFICATION_ID = "00000000-0000-0000-0000-000000000020";
    private static final UUID NONEXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-999999999999");

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @Sql("/sql/notification/insert-one-notification.sql")
    @DisplayName("GET /orders/{orderId}/notifications should return 200 with a page of notifications when successful")
    void findAllByOrderId_ReturnsPageOfNotifications_WhenSuccessful() {
        var expected = fileUtils.readResourceFile("notification/get-response-notification-list-200.json");

        var body = RestAssured.given()
                .when()
                .get("/orders/" + ORDER_ID + "/notifications")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body)
                .whenIgnoringPaths("content[*].id", "content[*].orderId", "content[*].customerId",
                        "content[*].message", "content[*].link", "content[*].sentAt",
                        "pageable", "last", "first", "size", "number", "sort", "numberOfElements", "empty")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /orders/{orderId}/notifications should return 200 with empty page when no notifications exist")
    void findAllByOrderId_ReturnsEmptyPage_WhenNoNotificationsExist() {
        RestAssured.given()
                .when()
                .get("/orders/" + NONEXISTENT_ID + "/notifications")
                .then()
                .log().all()
                .statusCode(200)
                .body("totalElements", Matchers.equalTo(0))
                .body("content", Matchers.empty());
    }

    @Test
    @Sql("/sql/notification/insert-one-notification.sql")
    @DisplayName("POST /notifications/{id}/resend should return 200 and create a new notification when successful")
    void resend_ReturnsOk_WhenSuccessful() {
        var body = RestAssured.given()
                .when()
                .post("/notifications/" + NOTIFICATION_ID + "/resend")
                .then()
                .log().all()
                .statusCode(200)
                .body("id", Matchers.notNullValue())
                .body("customerPhone", Matchers.equalTo("5511999999999"))
                .body("customerName", Matchers.equalTo("Maria Silva"))
                .body("message", Matchers.notNullValue())
                .body("link", Matchers.startsWith("https://wa.me/5511999999999"))
                .extract().body().asString();

        assertThat(notificationRepository.count()).isEqualTo(2);

        var resent = notificationRepository.findAll().stream()
                .filter(n -> !n.getId().toString().equals(NOTIFICATION_ID))
                .findFirst()
                .orElseThrow();
        assertThat(resent.getCustomerPhone()).isEqualTo("5511999999999");
        assertThat(resent.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /notifications/{id}/resend should return 404 when notification is not found")
    void resend_ReturnsNotFound_WhenNotificationNotFound() {
        var expected = fileUtils.readResourceFile("notification/get-response-notification-not-found-404.json");

        var body = RestAssured.given()
                .when()
                .post("/notifications/" + NONEXISTENT_ID + "/resend")
                .then()
                .log().all()
                .statusCode(404)
                .extract().body().asString();

        JsonAssertions.assertThatJson(body).isEqualTo(expected);
    }
}
