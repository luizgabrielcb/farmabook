package br.com.luizgabriel.farmabook.auth;

import br.com.luizgabriel.farmabook.auth.dto.UserGetResponse;
import br.com.luizgabriel.farmabook.auth.dto.UserPostRequest;
import br.com.luizgabriel.farmabook.auth.dto.UserPostResponse;
import br.com.luizgabriel.farmabook.auth.dto.UserPutResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "pinHash", source = "pinHash")
    User toUser(UserPostRequest userPostRequest, String pinHash);

    UserPostResponse toUserPostResponse(User user);

    UserGetResponse toUserGetResponse(User user);

    UserPutResponse toUserPutResponse(User user);
}
