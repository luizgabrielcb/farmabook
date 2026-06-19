package br.com.luizgabriel.farmabook.prescription;

import br.com.luizgabriel.farmabook.auth.User;
import br.com.luizgabriel.farmabook.customer.Customer;
import br.com.luizgabriel.farmabook.prescription.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PrescriptionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", expression = "java(customer != null ? customer.getId() : null)")
    @Mapping(target = "customerName", expression = "java(customer != null ? customer.getName() : null)")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.name")
    @Mapping(target = "observations", source = "request.observations")
    @Mapping(target = "items", ignore = true)
    Prescription toPrescription(PrescriptionPostRequest request, Customer customer, User createdBy);

    PrescriptionPostResponse toPrescriptionPostResponse(Prescription prescription);

    PrescriptionPutResponse toPrescriptionPutResponse(Prescription prescription);

    PrescriptionGetResponse toPrescriptionGetResponse(Prescription prescription);

    @Mapping(target = "items", source = "prescriptionItems")
    PrescriptionGetResponse toPrescriptionGetResponse(Prescription prescription, List<PrescriptionItem> prescriptionItems);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "prescription", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "receivedById", ignore = true)
    @Mapping(target = "receivedByName", ignore = true)
    @Mapping(target = "receivedAt", ignore = true)
    PrescriptionItem toPrescriptionItem(PrescriptionItemPostRequest request);

    List<PrescriptionItem> toPrescriptionItems(List<PrescriptionItemPostRequest> requests);

    PrescriptionItemGetResponse toPrescriptionItemGetResponse(PrescriptionItem item);

    List<PrescriptionItemGetResponse> toPrescriptionItemGetResponses(List<PrescriptionItem> items);
}
