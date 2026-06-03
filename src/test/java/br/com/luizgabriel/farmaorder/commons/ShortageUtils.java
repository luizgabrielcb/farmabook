package br.com.luizgabriel.farmaorder.commons;

import br.com.luizgabriel.farmaorder.stock.Category;
import br.com.luizgabriel.farmaorder.stock.shortage.Shortage;
import br.com.luizgabriel.farmaorder.stock.shortage.ShortageStatus;
import br.com.luizgabriel.farmaorder.stock.shortage.dto.*;

import java.time.Instant;
import java.util.UUID;

public class ShortageUtils {

    public static final UUID SHORTAGE_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");

    public Shortage newShortage() {
        return Shortage.builder()
                .id(SHORTAGE_ID)
                .product("Dipirona 500mg")
                .category(Category.GENERICO)
                .quantity(10)
                .status(ShortageStatus.PENDING)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .build();
    }

    public Shortage newOrderedShortage() {
        return Shortage.builder()
                .id(SHORTAGE_ID)
                .product("Dipirona 500mg")
                .category(Category.GENERICO)
                .quantity(10)
                .status(ShortageStatus.ORDERED)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .orderedById(UserUtils.USER_ID)
                .orderedByName("Test User")
                .orderedAt(Instant.now())
                .build();
    }

    public ShortagePostRequest newShortagePostRequest() {
        return new ShortagePostRequest("Dipirona 500mg", Category.GENERICO, 10);
    }

    public ShortagePutRequest newShortagePutRequest() {
        return new ShortagePutRequest("Paracetamol 750mg", Category.ETICO, 5);
    }

    public ShortagePostResponse newShortagePostResponse(Shortage shortage) {
        return new ShortagePostResponse(
                shortage.getId(),
                shortage.getProduct(),
                shortage.getCategory(),
                shortage.getQuantity(),
                shortage.getStatus(),
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
                shortage.getCreatedById(),
                shortage.getCreatedByName(),
                shortage.getOrderedById(),
                shortage.getOrderedByName(),
                shortage.getOrderedAt(),
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
                Instant.now()
        );
    }
}
