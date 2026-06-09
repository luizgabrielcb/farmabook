package br.com.luizgabriel.farmabook.compounding;

import br.com.luizgabriel.farmabook.auth.User;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingGetResponse;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingPostRequest;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingPostResponse;
import br.com.luizgabriel.farmabook.compounding.pharmacy.CompoundingPharmacy;
import br.com.luizgabriel.farmabook.customer.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompoundingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "pharmacyId", source = "pharmacy.id")
    @Mapping(target = "pharmacyName", source = "pharmacy.name")
    @Mapping(target = "pharmacyCity", source = "pharmacy.city")
    @Mapping(target = "quantity", source = "request.quantity")
    @Mapping(target = "value", source = "request.value")
    @Mapping(target = "observations", source = "request.observations")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "paymentStatus", constant = "TO_PAY")
    @Mapping(target = "notifiedAt", ignore = true)
    @Mapping(target = "createdById", source = "actor.id")
    @Mapping(target = "createdByName", source = "actor.name")
    @Mapping(target = "orderedById", ignore = true)
    @Mapping(target = "orderedByName", ignore = true)
    @Mapping(target = "orderedAt", ignore = true)
    @Mapping(target = "receivedById", ignore = true)
    @Mapping(target = "receivedByName", ignore = true)
    @Mapping(target = "receivedAt", ignore = true)
    @Mapping(target = "deliveredById", ignore = true)
    @Mapping(target = "deliveredByName", ignore = true)
    @Mapping(target = "deliveredAt", ignore = true)
    Compounding toCompounding(CompoundingPostRequest request, Customer customer, CompoundingPharmacy pharmacy, User actor);

    CompoundingPostResponse toCompoundingPostResponse(Compounding compounding);

    CompoundingGetResponse toCompoundingGetResponse(Compounding compounding);
}
