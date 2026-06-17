package br.com.luizgabriel.farmabook.compounding.pharmacy;

import br.com.luizgabriel.farmabook.compounding.pharmacy.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompoundingPharmacyMapper {

    @Mapping(target = "id", ignore = true)
    CompoundingPharmacy toCompoundingPharmacy(CompoundingPharmacyPostRequest request);

    CompoundingPharmacyPostResponse toCompoundingPharmacyPostResponse(CompoundingPharmacy pharmacy);

    CompoundingPharmacyPutResponse toCompoundingPharmacyPutResponse(CompoundingPharmacy pharmacy);

    CompoundingPharmacyGetResponse toCompoundingPharmacyGetResponse(CompoundingPharmacy pharmacy);
}
