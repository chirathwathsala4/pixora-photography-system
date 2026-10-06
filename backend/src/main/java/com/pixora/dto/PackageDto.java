package com.pixora.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

// Data Transfer Object representing photography package details and validation rules
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageDto {

    // Unique identifier for the package
    private Long packageId;

    // Display name of the photography package
    private String packageName;

    // Price in LKR with mandatory non-null validation
    @NotNull(message = "Package price must be a positive value.")
    // Validation constraint ensuring the price value is greater than zero
    @Positive(message = "Package price must be a positive value.")
    private BigDecimal priceLkr;

    // Brief description of services and inclusions offered in the package
    private String description;

    // Availability status indicating whether the package is currently active
    private Boolean isActive;

    // Custom getter to map packageId as "id" in JSON response payloads
    @com.fasterxml.jackson.annotation.JsonProperty("id")
    public Long getId() { return packageId; }

    // Custom getter to map packageName as "name" in JSON response payloads
    @com.fasterxml.jackson.annotation.JsonProperty("name")
    public String getName() { return packageName; }
}