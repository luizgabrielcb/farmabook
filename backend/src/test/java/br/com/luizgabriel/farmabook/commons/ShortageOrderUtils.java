package br.com.luizgabriel.farmabook.commons;

import br.com.luizgabriel.farmabook.catalog.Category;
import br.com.luizgabriel.farmabook.distributor.Distributor;
import br.com.luizgabriel.farmabook.shortage.*;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageGetResponse;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageOrderGetResponse;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageOrderItemRequest;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageOrderListResponse;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageOrderPostRequest;
import br.com.luizgabriel.farmabook.shortage.dto.ShortageOrderPutRequest;

import java.math.BigDecimal;
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

    public Distributor newDistributor() {
        return Distributor.builder()
                .id(DISTRIBUTOR_ID)
                .name("Test Distributor")
                .build();
    }

    public ShortageOrderItemRequest newShortageOrderItemRequest() {
        return new ShortageOrderItemRequest("Dipirona 500mg", Category.MEDICAMENTOS, 10, new BigDecimal("12.50"));
    }

    public ShortageOrderPostRequest newShortageOrderPostRequest() {
        return new ShortageOrderPostRequest(
                ShortageType.WANIA,
                DISTRIBUTOR_ID,
                List.of(newShortageOrderItemRequest()),
                "Observações"
        );
    }

    public ShortageOrderListResponse newShortageOrderListResponse(ShortageOrder order) {
        return new ShortageOrderListResponse(
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
                Instant.now(),
                Instant.now()
        );
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
