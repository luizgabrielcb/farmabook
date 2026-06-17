package br.com.luizgabriel.farmabook.commons;

import br.com.luizgabriel.farmabook.compounding.pharmacy.CompoundingPharmacy;
import br.com.luizgabriel.farmabook.compounding.pharmacy.dto.*;

import java.time.Instant;
import java.util.UUID;

public class CompoundingPharmacyUtils {

    public static final UUID PHARMACY_ID = UUID.fromString("00000000-0000-0000-0000-000000000060");

    public CompoundingPharmacy newPharmacy() {
        return CompoundingPharmacy.builder()
                .id(PHARMACY_ID)
                .name("Farmácia Magistral Central")
                .city("São Paulo")
                .build();
    }

    public CompoundingPharmacyPostRequest newPharmacyPostRequest() {
        return new CompoundingPharmacyPostRequest("Farmácia Magistral Central", "São Paulo");
    }

    public CompoundingPharmacyPutRequest newPharmacyPutRequest() {
        return new CompoundingPharmacyPutRequest("Farmácia Magistral Norte", "Campinas");
    }

    public CompoundingPharmacyPostResponse newPharmacyPostResponse(CompoundingPharmacy pharmacy) {
        return new CompoundingPharmacyPostResponse(
                pharmacy.getId(),
                pharmacy.getName(),
                pharmacy.getCity(),
                Instant.now()
        );
    }

    public CompoundingPharmacyPutResponse newPharmacyPutResponse(CompoundingPharmacy pharmacy) {
        return new CompoundingPharmacyPutResponse(
                pharmacy.getId(),
                pharmacy.getName(),
                pharmacy.getCity(),
                Instant.now()
        );
    }

    public CompoundingPharmacyGetResponse newPharmacyGetResponse(CompoundingPharmacy pharmacy) {
        return new CompoundingPharmacyGetResponse(
                pharmacy.getId(),
                pharmacy.getName(),
                pharmacy.getCity(),
                Instant.now()
        );
    }
}
