package br.com.luizgabriel.farmabook.commons;

import br.com.luizgabriel.farmabook.distributor.Distributor;
import br.com.luizgabriel.farmabook.distributor.dto.DistributorGetResponse;
import br.com.luizgabriel.farmabook.distributor.dto.DistributorPostRequest;

import java.time.Instant;
import java.util.UUID;

public class DistributorUtils {

    public static final UUID DISTRIBUTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000070");

    public Distributor newDistributor() {
        return Distributor.builder()
                .id(DISTRIBUTOR_ID)
                .name("Distribuidora Central")
                .build();
    }

    public DistributorPostRequest newDistributorPostRequest() {
        return new DistributorPostRequest("Distribuidora Central");
    }

    public DistributorPostRequest newDistributorPutRequest() {
        return new DistributorPostRequest("Distribuidora Norte");
    }

    public DistributorGetResponse newDistributorGetResponse(Distributor distributor) {
        return new DistributorGetResponse(
                distributor.getId(),
                distributor.getName(),
                Instant.now()
        );
    }
}
