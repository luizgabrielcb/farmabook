package br.com.luizgabriel.farmabook.customer;

import br.com.luizgabriel.farmabook.customer.dto.CustomerGetResponse;
import br.com.luizgabriel.farmabook.customer.dto.CustomerPostRequest;
import br.com.luizgabriel.farmabook.customer.dto.CustomerPostResponse;
import br.com.luizgabriel.farmabook.customer.dto.CustomerPutResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    Customer toCustomer(CustomerPostRequest request);

    CustomerPostResponse toCustomerPostResponse(Customer user);

    CustomerGetResponse toCustomerGetResponse(Customer user);

    CustomerPutResponse toCustomerPutResponse(Customer user);
}
