package br.com.luizgabriel.farmaorder.exception;

import lombok.Builder;

@Builder
public record DefaultErrorMessage(String message, int status) {
}
