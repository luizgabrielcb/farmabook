package br.com.luizgabriel.farmabook.exception;

import lombok.Builder;

@Builder
public record DefaultErrorMessage(String message, int status) {
}
