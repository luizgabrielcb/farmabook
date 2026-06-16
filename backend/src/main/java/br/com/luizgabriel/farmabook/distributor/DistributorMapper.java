package br.com.luizgabriel.farmabook.distributor;
import br.com.luizgabriel.farmabook.distributor.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DistributorMapper {
    @Mapping(target = "id", ignore = true)
    Distributor toDistributor(DistributorPostRequest request);
    DistributorGetResponse toDistributorGetResponse(Distributor distributor);
}
