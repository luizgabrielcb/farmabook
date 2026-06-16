package br.com.luizgabriel.farmabook.distributor.dto;
import java.time.Instant;
import java.util.UUID;
public record DistributorGetResponse(UUID id, String name, Instant createdAt) {}
