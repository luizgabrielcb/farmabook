package br.com.luizgabriel.farmaorder.notification;

import br.com.luizgabriel.farmaorder.notification.dto.NotificationGetResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "customer.id", target = "customerId")
    NotificationGetResponse toNotificationGetResponse(Notification notification);
}
