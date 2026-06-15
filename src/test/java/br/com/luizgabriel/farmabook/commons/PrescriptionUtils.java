package br.com.luizgabriel.farmabook.commons;

import br.com.luizgabriel.farmabook.prescription.*;
import br.com.luizgabriel.farmabook.prescription.dto.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PrescriptionUtils {

    public static final UUID PRESCRIPTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    public static final UUID ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000031");
    public static final UUID OTHER_ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000032");

    public PrescriptionItem newItem(UUID id, PrescriptionItemStatus status) {
        return PrescriptionItem.builder()
                .id(id)
                .product("Dipirona 500mg")
                .quantity(2)
                .batch("LOTE-001")
                .expiry("12/2025")
                .status(status)
                .build();
    }

    public PrescriptionItem newPendingItem() {
        return newItem(ITEM_ID, PrescriptionItemStatus.PENDING);
    }

    public PrescriptionItem newReceivedItem() {
        return newItem(ITEM_ID, PrescriptionItemStatus.RECEIVED);
    }

    private Prescription buildPrescription(PrescriptionStatus status, PrescriptionItem... items) {
        return Prescription.builder()
                .id(PRESCRIPTION_ID)
                .customerId(CustomerUtils.CUSTOMER_ID)
                .customerName("Test Customer")
                .status(status)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .items(new ArrayList<>(Arrays.asList(items)))
                .build();
    }

    public Prescription newPrescription() {
        return buildPrescription(PrescriptionStatus.PENDING, newPendingItem());
    }

    public Prescription newFinishedPrescription() {
        return buildPrescription(PrescriptionStatus.FINISHED, newReceivedItem());
    }

    public Prescription newPrescriptionWithReceivedItem() {
        return buildPrescription(PrescriptionStatus.PENDING,
                newPendingItem(),
                newItem(OTHER_ITEM_ID, PrescriptionItemStatus.RECEIVED));
    }

    public PrescriptionPostRequest newPrescriptionPostRequest() {
        return new PrescriptionPostRequest(
                CustomerUtils.CUSTOMER_ID,
                List.of(newPrescriptionItemPostRequest()),
                null);
    }

    public PrescriptionItemPostRequest newPrescriptionItemPostRequest() {
        return new PrescriptionItemPostRequest("Dipirona 500mg", 2, "LOTE-001", "12/2025");
    }

    public PrescriptionItemPutRequest newPrescriptionItemPutRequest() {
        return new PrescriptionItemPutRequest("Paracetamol 750mg", 3, "LOTE-999", "06/2026");
    }

    public PrescriptionPutRequest newPrescriptionPutRequest() {
        return new PrescriptionPutRequest(CustomerUtils.OTHER_CUSTOMER_ID, "Observação atualizada");
    }

    public PrescriptionPostResponse newPrescriptionPostResponse(Prescription p) {
        return new PrescriptionPostResponse(p.getId(), p.getCustomerId(), p.getCustomerName(),
                p.getStatus(), p.getCreatedById(), p.getCreatedByName(), p.getObservations(), Instant.now(), List.of());
    }

    public PrescriptionPutResponse newPrescriptionPutResponse(Prescription p) {
        return new PrescriptionPutResponse(p.getId(), p.getCustomerId(), p.getCustomerName(),
                p.getStatus(), Instant.now());
    }

    public PrescriptionGetResponse newPrescriptionGetResponse(Prescription p) {
        return new PrescriptionGetResponse(p.getId(), p.getCustomerId(), p.getCustomerName(),
                p.getStatus(), p.getCreatedById(), p.getCreatedByName(), p.getObservations(),
                List.of(), Instant.now(), Instant.now());
    }

    public PrescriptionItemGetResponse newPrescriptionItemGetResponse(PrescriptionItem item) {
        return new PrescriptionItemGetResponse(item.getId(), item.getProduct(), item.getQuantity(),
                item.getBatch(), item.getExpiry(), item.getStatus(),
                item.getReceivedById(), item.getReceivedByName(), item.getReceivedAt(),
                Instant.now(), Instant.now());
    }
}
