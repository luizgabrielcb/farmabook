package br.com.luizgabriel.farmabook.commons;

import br.com.luizgabriel.farmabook.stock.Category;
import br.com.luizgabriel.farmabook.stock.shortage.Shortage;
import br.com.luizgabriel.farmabook.stock.shortage.ShortageStatus;
import br.com.luizgabriel.farmabook.stock.shortage.ShortageType;
import br.com.luizgabriel.farmabook.stock.shortage.dto.*;

import java.time.Instant;
import java.util.UUID;

public class ShortageUtils {

    public static final UUID SHORTAGE_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");

    public Shortage newShortage() {
        return Shortage.builder()
                .id(SHORTAGE_ID)
                .product("Dipirona 500mg")
                .category(Category.MEDICAMENTOS)
                .quantity(10)
                .shortageType(ShortageType.WANIA)
                .status(ShortageStatus.PENDING)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .build();
    }

    public Shortage newShortageWithOrderId(java.util.UUID shortageOrderId) {
        return Shortage.builder()
                .id(SHORTAGE_ID)
                .product("Dipirona 500mg")
                .category(Category.MEDICAMENTOS)
                .quantity(10)
                .shortageType(ShortageType.WANIA)
                .status(ShortageStatus.PENDING)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .shortageOrderId(shortageOrderId)
                .build();
    }

    public Shortage newOrderedShortage() {
        return Shortage.builder()
                .id(SHORTAGE_ID)
                .product("Dipirona 500mg")
                .category(Category.MEDICAMENTOS)
                .quantity(10)
                .shortageType(ShortageType.WANIA)
                .status(ShortageStatus.ORDERED)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .orderedById(UserUtils.USER_ID)
                .orderedByName("Test User")
                .orderedAt(Instant.now())
                .build();
    }

    public ShortagePostRequest newShortagePostRequest() {
        return new ShortagePostRequest("Dipirona 500mg", Category.MEDICAMENTOS, 10, ShortageType.WANIA);
    }

    public ShortagePutRequest newShortagePutRequest() {
        return new ShortagePutRequest("Paracetamol 750mg", Category.PERFUMARIA, 5, null);
    }

    public ShortagePostResponse newShortagePostResponse(Shortage shortage) {
        return new ShortagePostResponse(
                shortage.getId(),
                shortage.getProduct(),
                shortage.getCategory(),
                shortage.getQuantity(),
                shortage.getStatus(),
                shortage.getShortageType(),
                shortage.getCreatedById(),
                shortage.getCreatedByName(),
                Instant.now()
        );
    }

    public ShortageGetResponse newShortageGetResponse(Shortage shortage) {
        return new ShortageGetResponse(
                shortage.getId(),
                shortage.getProduct(),
                shortage.getCategory(),
                shortage.getQuantity(),
                shortage.getStatus(),
                shortage.getShortageType(),
                shortage.getCreatedById(),
                shortage.getCreatedByName(),
                shortage.getOrderedById(),
                shortage.getOrderedByName(),
                shortage.getOrderedAt(),
                shortage.getShortageOrderId(),
                shortage.getCostPrice(),
                Instant.now(),
                Instant.now()
        );
    }

    public ShortagePutResponse newShortagePutResponse(Shortage shortage) {
        return new ShortagePutResponse(
                shortage.getId(),
                shortage.getProduct(),
                shortage.getCategory(),
                shortage.getQuantity(),
                shortage.getStatus(),
                shortage.getShortageType(),
                Instant.now()
        );
    }
}
