package br.com.luizgabriel.farmaorder.commons;

import br.com.luizgabriel.farmaorder.customer.Customer;
import br.com.luizgabriel.farmaorder.customer.dto.*;

import java.time.Instant;
import java.util.UUID;

public class CustomerUtils {

    public static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    public static final UUID OTHER_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");

    public Customer newCustomer() {
        return Customer.builder()
                .id(CUSTOMER_ID)
                .name("Test Customer")
                .phoneNumber("+5511999999999")
                .build();
    }

    public Customer newCustomerWithoutPhone() {
        return Customer.builder()
                .id(CUSTOMER_ID)
                .name("Test Customer")
                .phoneNumber(null)
                .build();
    }

    public Customer newOtherCustomer() {
        return Customer.builder()
                .id(OTHER_CUSTOMER_ID)
                .name("Other Customer")
                .phoneNumber("+5511888888888")
                .build();
    }

    public CustomerPostRequest newCustomerPostRequest() {
        return new CustomerPostRequest("Test Customer", "+5511999999999");
    }

    public CustomerPostRequest newCustomerPostRequestWithoutPhone() {
        return new CustomerPostRequest("Test Customer", null);
    }

    public CustomerPostResponse newCustomerPostResponse(Customer customer) {
        return new CustomerPostResponse(customer.getId(), customer.getName(), customer.getPhoneNumber(), Instant.now());
    }

    public CustomerGetResponse newCustomerGetResponse(Customer customer) {
        return new CustomerGetResponse(customer.getId(), customer.getName(), customer.getPhoneNumber(), Instant.now(), Instant.now());
    }

    public CustomerPutRequest newCustomerPutRequest() {
        return new CustomerPutRequest("Updated Customer", "+5511777777777");
    }

    public CustomerPutRequest newCustomerPutRequestWithoutPhone() {
        return new CustomerPutRequest("Updated Customer", null);
    }

    public CustomerPutResponse newCustomerPutResponse(Customer customer) {
        return new CustomerPutResponse(customer.getId(), customer.getName(), customer.getPhoneNumber(), Instant.now());
    }
}
