package br.com.luizgabriel.farmabook.notification;

import br.com.luizgabriel.farmabook.notification.dto.NotificationGetResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "compounding.id", target = "compoundingId")
    @Mapping(source = "customer.id", target = "customerId")
    NotificationGetResponse toNotificationGetResponse(Notification notification);
}
