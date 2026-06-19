package br.com.luizgabriel.farmabook.notification;

import br.com.luizgabriel.farmabook.commons.CustomerUtils;
import br.com.luizgabriel.farmabook.commons.NotificationUtils;
import br.com.luizgabriel.farmabook.compounding.Compounding;
import br.com.luizgabriel.farmabook.customer.Customer;
import br.com.luizgabriel.farmabook.customer.CustomerService;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService service;

    @InjectMocks
    private NotificationUtils utils;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private NotificationMapper notificationMapper;

    @Test
    @DisplayName("generateForOrderReceived should persist notification and return response when successful")
    void generateForOrderReceived_ReturnsNotificationGetResponse_WhenSuccessful() {
        var order = utils.newOrder();
        var customer = utils.newCustomer();
        var saved = utils.newNotification(order, customer);
        var response = utils.newNotificationGetResponse(saved);

        BDDMockito.when(customerService.findByIdOrThrowNotFound(order.getCustomerId())).thenReturn(customer);
        BDDMockito.when(notificationRepository.save(ArgumentMatchers.any(Notification.class))).thenReturn(saved);
        BDDMockito.when(notificationMapper.toNotificationGetResponse(saved)).thenReturn(response);

        var result = service.generateForOrderReceived(order);

        assertThat(result).contains(response);
        BDDMockito.then(notificationRepository).should().save(ArgumentMatchers.any(Notification.class));
    }

    @Test
    @DisplayName("generateForOrderReceived should prepend Brazil country code when phone has no country code")
    void generateForOrderReceived_PrependsBrazilCountryCode_WhenPhoneHasNoCountryCode() {
        var order = utils.newOrder();
        var customer = utils.newCustomerWithLocalPhone();

        var saved = utils.newNotification(order, customer);

        BDDMockito.when(customerService.findByIdOrThrowNotFound(order.getCustomerId())).thenReturn(customer);
        BDDMockito.when(notificationRepository.save(ArgumentMatchers.any(Notification.class))).thenReturn(saved);
        BDDMockito.when(notificationMapper.toNotificationGetResponse(saved)).thenReturn(utils.newNotificationGetResponse(saved));

        service.generateForOrderReceived(order);

        var captor = ArgumentCaptor.forClass(Notification.class);
        BDDMockito.then(notificationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getCustomerPhone()).isEqualTo("5511999999999");
    }

    @Test
    @DisplayName("generateForOrderReceived should use the single name in the message when customer has only one name")
    void generateForOrderReceived_UsesSingleName_WhenCustomerHasOnlyOneName() {
        var order = utils.newOrder();
        var customer = utils.newCustomerWithSingleName();

        var saved = utils.newNotification(order, customer);

        BDDMockito.when(customerService.findByIdOrThrowNotFound(order.getCustomerId())).thenReturn(customer);
        BDDMockito.when(notificationRepository.save(ArgumentMatchers.any(Notification.class))).thenReturn(saved);
        BDDMockito.when(notificationMapper.toNotificationGetResponse(saved)).thenReturn(utils.newNotificationGetResponse(saved));

        service.generateForOrderReceived(order);

        var captor = ArgumentCaptor.forClass(Notification.class);
        BDDMockito.then(notificationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getMessage()).contains(", Joao!");
    }

    @Test
    @DisplayName("generateForOrderReceived should throw NotFoundException when customer is not found")
    void generateForOrderReceived_ThrowsNotFoundException_WhenCustomerNotFound() {
        var order = utils.newOrder();

        BDDMockito.when(customerService.findByIdOrThrowNotFound(order.getCustomerId()))
                .thenThrow(new NotFoundException("Customer not found"));

        assertThatThrownBy(() -> service.generateForOrderReceived(order))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(notificationRepository).should(Mockito.never()).save(ArgumentMatchers.any(Notification.class));
    }

    @Test
    @DisplayName("generateForCompoundingReceived should persist notification and return response when successful")
    void generateForCompoundingReceived_ReturnsNotificationGetResponse_WhenSuccessful() {
        var compounding = newCompounding();
        var customer = utils.newCustomer();
        var saved = utils.newNotification(utils.newOrder(), customer);
        var response = utils.newNotificationGetResponse(saved);

        BDDMockito.when(customerService.findByIdOrThrowNotFound(compounding.getCustomerId())).thenReturn(customer);
        BDDMockito.when(notificationRepository.save(ArgumentMatchers.any(Notification.class))).thenReturn(saved);
        BDDMockito.when(notificationMapper.toNotificationGetResponse(saved)).thenReturn(response);

        var result = service.generateForCompoundingReceived(compounding);

        assertThat(result).contains(response);

        var captor = ArgumentCaptor.forClass(Notification.class);
        BDDMockito.then(notificationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getMessage()).contains("manipulação");
    }

    @Test
    @DisplayName("generateForCompoundingReceived should return empty and not persist when customer has no phone")
    void generateForCompoundingReceived_ReturnsEmpty_WhenCustomerHasNoPhone() {
        var compounding = newCompounding();
        var customer = Customer.builder()
                .id(CustomerUtils.CUSTOMER_ID)
                .name("Test Customer")
                .phoneNumber(null)
                .build();

        BDDMockito.when(customerService.findByIdOrThrowNotFound(compounding.getCustomerId())).thenReturn(customer);

        var result = service.generateForCompoundingReceived(compounding);

        assertThat(result).isEmpty();
        BDDMockito.then(notificationRepository).should(Mockito.never()).save(ArgumentMatchers.any(Notification.class));
    }

    @Test
    @DisplayName("generateForCompoundingReceived should throw NotFoundException when customer is not found")
    void generateForCompoundingReceived_ThrowsNotFoundException_WhenCustomerNotFound() {
        var compounding = newCompounding();

        BDDMockito.when(customerService.findByIdOrThrowNotFound(compounding.getCustomerId()))
                .thenThrow(new NotFoundException("Customer not found"));

        assertThatThrownBy(() -> service.generateForCompoundingReceived(compounding))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(notificationRepository).should(Mockito.never()).save(ArgumentMatchers.any(Notification.class));
    }

    @Test
    @DisplayName("findAllByCompoundingId should return a page of notifications when successful")
    void findAllByCompoundingId_ReturnsPageOfNotifications_WhenSuccessful() {
        var pageable = Pageable.ofSize(10);
        var compoundingId = UUID.randomUUID();
        var notification = utils.newNotification(utils.newOrder(), utils.newCustomer());
        var response = utils.newNotificationGetResponse(notification);
        var page = new PageImpl<>(List.of(notification));

        BDDMockito.when(notificationRepository.findAllByCompoundingId(compoundingId, pageable)).thenReturn(page);
        BDDMockito.when(notificationMapper.toNotificationGetResponse(notification)).thenReturn(response);

        var result = service.findAllByCompoundingId(compoundingId, pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("findAllByCompoundingId should return an empty page when no notifications exist for the compounding")
    void findAllByCompoundingId_ReturnsEmptyPage_WhenNoNotificationsExist() {
        var compoundingId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        BDDMockito.when(notificationRepository.findAllByCompoundingId(compoundingId, pageable)).thenReturn(Page.empty());

        var result = service.findAllByCompoundingId(compoundingId, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    private Compounding newCompounding() {
        return Compounding.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000070"))
                .customerId(CustomerUtils.CUSTOMER_ID)
                .customerName("Test Customer")
                .quantity(2)
                .build();
    }

    @Test
    @DisplayName("findAllByOrderId should return a page of notifications when successful")
    void findAllByOrderId_ReturnsPageOfNotifications_WhenSuccessful() {
        var pageable = Pageable.ofSize(10);
        var order = utils.newOrder();
        var customer = utils.newCustomer();
        var notification = utils.newNotification(order, customer);
        var response = utils.newNotificationGetResponse(notification);
        var page = new PageImpl<>(List.of(notification));

        BDDMockito.when(notificationRepository.findAllByOrderId(order.getId(), pageable)).thenReturn(page);
        BDDMockito.when(notificationMapper.toNotificationGetResponse(notification)).thenReturn(response);

        var result = service.findAllByOrderId(order.getId(), pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("findAllByOrderId should return an empty page when no notifications exist for the order")
    void findAllByOrderId_ReturnsEmptyPage_WhenNoNotificationsExist() {
        var orderId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        BDDMockito.when(notificationRepository.findAllByOrderId(orderId, pageable)).thenReturn(Page.empty());

        var result = service.findAllByOrderId(orderId, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("resend should persist a new notification and return response when successful")
    void resend_ReturnsNotificationGetResponse_WhenSuccessful() {
        var order = utils.newOrder();
        var customer = utils.newCustomer();
        var original = utils.newNotification(order, customer);
        var saved = utils.newNotification(order, customer);
        var response = utils.newNotificationGetResponse(saved);

        BDDMockito.when(notificationRepository.findById(original.getId())).thenReturn(Optional.of(original));
        BDDMockito.when(customerService.findByIdOrThrowNotFound(order.getCustomerId())).thenReturn(customer);
        BDDMockito.when(notificationRepository.save(ArgumentMatchers.any(Notification.class))).thenReturn(saved);
        BDDMockito.when(notificationMapper.toNotificationGetResponse(saved)).thenReturn(response);

        var result = service.resend(original.getId());

        assertThat(result).isEqualTo(response);
        BDDMockito.then(notificationRepository).should().save(ArgumentMatchers.any(Notification.class));
    }

    @Test
    @DisplayName("resend should throw NotFoundException when notification is not found")
    void resend_ThrowsNotFoundException_WhenNotificationNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resend(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(notificationRepository).should(Mockito.never()).save(ArgumentMatchers.any(Notification.class));
    }
}
