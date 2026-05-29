package br.com.luizgabriel.farmaorder.auth.exception;

import lombok.Builder;

@Builder
public record DefaultErrorMessage(String message, int status) {
}
