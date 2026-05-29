package br.com.luizgabriel.farmaorder.auth;

import br.com.luizgabriel.farmaorder.auth.dto.UserGetResponse;
import br.com.luizgabriel.farmaorder.auth.dto.UserPostRequest;
import br.com.luizgabriel.farmaorder.auth.dto.UserPostResponse;
import br.com.luizgabriel.farmaorder.auth.dto.UserPutResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "pinHash", source = "pinHash")
    User toUser(UserPostRequest request, String pinHash);

    UserPostResponse toUserPostResponse(User user);

    UserGetResponse toUserGetResponse(User user);

    UserPutResponse toUserPutResponse(User user);
}
