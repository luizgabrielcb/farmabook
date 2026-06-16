package br.com.luizgabriel.farmabook.commons;

import br.com.luizgabriel.farmabook.compounding.Compounding;
import br.com.luizgabriel.farmabook.compounding.CompoundingStatus;
import br.com.luizgabriel.farmabook.compounding.PaymentStatus;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingGetResponse;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingPostRequest;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingPostResponse;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingPutRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CompoundingUtils {

    public static final UUID COMPOUNDING_ID = UUID.fromString("00000000-0000-0000-0000-000000000070");

    public Compounding newCompounding() {
        return Compounding.builder()
                .id(COMPOUNDING_ID)
                .quantity(2)
                .customerId(CustomerUtils.CUSTOMER_ID)
                .customerName("Test Customer")
                .pharmacyId(CompoundingPharmacyUtils.PHARMACY_ID)
                .pharmacyName("Farmácia Magistral Central")
                .pharmacyCity("São Paulo")
                .value(new BigDecimal("150.00"))
                .observations("Sem observações")
                .status(CompoundingStatus.PENDING)
                .paymentStatus(PaymentStatus.TO_PAY)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .build();
    }

    public Compounding newOrderedCompounding() {
        return Compounding.builder()
                .id(COMPOUNDING_ID)
                .quantity(2)
                .customerId(CustomerUtils.CUSTOMER_ID)
                .customerName("Test Customer")
                .pharmacyId(CompoundingPharmacyUtils.PHARMACY_ID)
                .pharmacyName("Farmácia Magistral Central")
                .pharmacyCity("São Paulo")
                .status(CompoundingStatus.ORDERED)
                .paymentStatus(PaymentStatus.TO_PAY)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .orderedById(UserUtils.USER_ID)
                .orderedByName("Test User")
                .orderedAt(Instant.now())
                .build();
    }

    public Compounding newReceivedCompounding() {
        return Compounding.builder()
                .id(COMPOUNDING_ID)
                .quantity(2)
                .customerId(CustomerUtils.CUSTOMER_ID)
                .customerName("Test Customer")
                .pharmacyId(CompoundingPharmacyUtils.PHARMACY_ID)
                .pharmacyName("Farmácia Magistral Central")
                .pharmacyCity("São Paulo")
                .status(CompoundingStatus.RECEIVED)
                .paymentStatus(PaymentStatus.TO_PAY)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .orderedById(UserUtils.USER_ID)
                .orderedByName("Test User")
                .orderedAt(Instant.now())
                .receivedById(UserUtils.USER_ID)
                .receivedByName("Test User")
                .receivedAt(Instant.now())
                .build();
    }

    public Compounding newDeliveredCompounding() {
        return Compounding.builder()
                .id(COMPOUNDING_ID)
                .quantity(2)
                .customerId(CustomerUtils.CUSTOMER_ID)
                .customerName("Test Customer")
                .pharmacyId(CompoundingPharmacyUtils.PHARMACY_ID)
                .pharmacyName("Farmácia Magistral Central")
                .pharmacyCity("São Paulo")
                .status(CompoundingStatus.DELIVERED)
                .paymentStatus(PaymentStatus.TO_PAY)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .orderedById(UserUtils.USER_ID)
                .orderedByName("Test User")
                .orderedAt(Instant.now())
                .receivedById(UserUtils.USER_ID)
                .receivedByName("Test User")
                .receivedAt(Instant.now())
                .deliveredById(UserUtils.USER_ID)
                .deliveredByName("Test User")
                .deliveredAt(Instant.now())
                .build();
    }

    public Compounding newCompoundingWithPaymentStatus(PaymentStatus paymentStatus) {
        return Compounding.builder()
                .id(COMPOUNDING_ID)
                .quantity(2)
                .customerId(CustomerUtils.CUSTOMER_ID)
                .customerName("Test Customer")
                .pharmacyId(CompoundingPharmacyUtils.PHARMACY_ID)
                .pharmacyName("Farmácia Magistral Central")
                .pharmacyCity("São Paulo")
                .status(CompoundingStatus.PENDING)
                .paymentStatus(paymentStatus)
                .createdById(UserUtils.USER_ID)
                .createdByName("Test User")
                .build();
    }

    public CompoundingPostRequest newCompoundingPostRequest() {
        return new CompoundingPostRequest(
                2,
                CustomerUtils.CUSTOMER_ID,
                CompoundingPharmacyUtils.PHARMACY_ID,
                new BigDecimal("150.00"),
                "Sem observações",
                null
        );
    }

    public CompoundingPutRequest newCompoundingPutRequest() {
        return new CompoundingPutRequest(
                3,
                CustomerUtils.CUSTOMER_ID,
                CompoundingPharmacyUtils.PHARMACY_ID,
                new BigDecimal("200.00"),
                "Observação atualizada"
        );
    }

    public CompoundingPostResponse newCompoundingPostResponse(Compounding compounding) {
        return new CompoundingPostResponse(
                compounding.getId(),
                compounding.getQuantity(),
                compounding.getCustomerName(),
                compounding.getPharmacyName(),
                compounding.getStatus(),
                compounding.getPaymentStatus(),
                Instant.now()
        );
    }

    public CompoundingGetResponse newCompoundingGetResponse(Compounding compounding) {
        return new CompoundingGetResponse(
                compounding.getId(),
                compounding.getQuantity(),
                compounding.getCustomerId(),
                compounding.getCustomerName(),
                compounding.getPharmacyId(),
                compounding.getPharmacyName(),
                compounding.getPharmacyCity(),
                compounding.getValue(),
                compounding.getObservations(),
                compounding.getStatus(),
                compounding.getPaymentStatus(),
                compounding.getNotifiedAt(),
                compounding.getCreatedById(),
                compounding.getCreatedByName(),
                compounding.getOrderedById(),
                compounding.getOrderedByName(),
                compounding.getOrderedAt(),
                compounding.getReceivedById(),
                compounding.getReceivedByName(),
                compounding.getReceivedAt(),
                compounding.getDeliveredById(),
                compounding.getDeliveredByName(),
                compounding.getDeliveredAt(),
                compounding.getPaymentChangedById(),
                compounding.getPaymentChangedByName(),
                compounding.getPaymentChangedAt(),
                Instant.now(),
                Instant.now()
        );
    }
}
