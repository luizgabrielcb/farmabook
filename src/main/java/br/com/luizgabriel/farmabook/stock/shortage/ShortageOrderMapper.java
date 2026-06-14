package br.com.luizgabriel.farmabook.stock.shortage;

import br.com.luizgabriel.farmabook.auth.User;
import br.com.luizgabriel.farmabook.stock.shortage.dto.ShortageGetResponse;
import br.com.luizgabriel.farmabook.stock.shortage.dto.ShortageOrderGetResponse;
import br.com.luizgabriel.farmabook.stock.shortage.dto.ShortageOrderListResponse;
import br.com.luizgabriel.farmabook.stock.shortage.dto.ShortageOrderPostRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ShortageOrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shortageType", source = "request.shortageType")
    @Mapping(target = "distributorId", source = "request.distributorId")
    @Mapping(target = "distributorName", source = "distributorName")
    @Mapping(target = "observations", source = "request.observations")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.name")
    @Mapping(target = "orderedById", ignore = true)
    @Mapping(target = "orderedByName", ignore = true)
    @Mapping(target = "orderedAt", ignore = true)
    ShortageOrder toShortageOrder(ShortageOrderPostRequest request, String distributorName, User createdBy);

    ShortageOrderListResponse toShortageOrderListResponse(ShortageOrder order);

    @Mapping(target = "shortages", source = "shortages")
    ShortageOrderGetResponse toShortageOrderGetResponse(ShortageOrder order, List<ShortageGetResponse> shortages);
}
