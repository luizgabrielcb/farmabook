package br.com.luizgabriel.farmabook.commons;

import br.com.luizgabriel.farmabook.shortage.*;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageGetResponse;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageOrderGetResponse;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageOrderPutRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ShortageOrderUtils {

    public static final UUID SHORTAGE_ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000060");
    public static final UUID DISTRIBUTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    public ShortageOrder newShortageOrder() {
        return ShortageOrder.builder()
                .id(SHORTAGE_ORDER_ID)
                .shortageType(ShortageType.WANIA)
                .distributorId(DISTRIBUTOR_ID)
                .distributorName("Test Distributor")
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .build();
    }

    public ShortageOrder newOrderedShortageOrder() {
        return ShortageOrder.builder()
                .id(SHORTAGE_ORDER_ID)
                .shortageType(ShortageType.WANIA)
                .distributorId(DISTRIBUTOR_ID)
                .distributorName("Test Distributor")
                .status(ShortageOrderStatus.ORDERED)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .orderedById(UserUtils.USER_ID)
                .orderedByName("Test User")
                .orderedAt(Instant.now())
                .build();
    }

    public ShortageOrderPutRequest newShortageOrderPutRequest() {
        return new ShortageOrderPutRequest(DISTRIBUTOR_ID, "Observações atualizadas");
    }

    public ShortageOrderGetResponse newShortageOrderGetResponse(ShortageOrder order, List<ShortageGetResponse> shortages) {
        return new ShortageOrderGetResponse(
                order.getId(),
                order.getShortageType(),
                order.getDistributorId(),
                order.getDistributorName(),
                order.getStatus(),
                order.getCreatedById(),
                order.getCreatedByName(),
                order.getOrderedById(),
                order.getOrderedByName(),
                order.getOrderedAt(),
                order.getObservations(),
                shortages,
                Instant.now(),
                Instant.now()
        );
    }
}
