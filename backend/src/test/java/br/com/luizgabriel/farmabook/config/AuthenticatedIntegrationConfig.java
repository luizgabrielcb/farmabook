package br.com.luizgabriel.farmabook.config;

import io.restassured.http.Header;

public abstract class AuthenticatedIntegrationConfig extends IntegrationTestConfig {

    protected Header authPinHeader() {
        return new Header("X-Auth-Pin", "1234");
    }
}
