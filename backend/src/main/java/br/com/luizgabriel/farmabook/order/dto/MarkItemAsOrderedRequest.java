package br.com.luizgabriel.farmabook.order.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record MarkItemAsOrderedRequest(
    @NotNull(message = "distributorId is required")
    UUID distributorId
) {}
