package br.com.luizgabriel.farmaorder.stock.shortage;

import br.com.luizgabriel.farmaorder.auth.User;
import br.com.luizgabriel.farmaorder.stock.shortage.dto.ShortageGetResponse;
import br.com.luizgabriel.farmaorder.stock.shortage.dto.ShortagePostRequest;
import br.com.luizgabriel.farmaorder.stock.shortage.dto.ShortagePostResponse;
import br.com.luizgabriel.farmaorder.stock.shortage.dto.ShortagePutResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShortageMapper {

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.name")
    @Mapping(target = "status", constant = "PENDING")
    Shortage toShortage(ShortagePostRequest request, User createdBy);

    ShortagePostResponse toShortagePostResponse(Shortage shortage);

    ShortagePutResponse toShortagePutResponse(Shortage shortage);

    ShortageGetResponse toShortageGetResponse(Shortage shortage);
}
