package com.pixora.dto; // DTO package

import jakarta.validation.constraints.FutureOrPresent; // Date cannot be in the past
import jakarta.validation.constraints.NotBlank; // String cannot be empty
import jakarta.validation.constraints.NotNull; // Value is required
import lombok.Data; // Generates getters/setters

import java.time.LocalDate; // Date
import java.time.LocalTime; // Time

@Data
public class BookingRequest {

    private Long photographerId; // Selected photographer

    @NotNull
    private Long packageId; // Selected package

    @NotNull
    @FutureOrPresent
    private LocalDate eventDate; // Event date

    @NotNull
    private LocalTime eventTime; // Event time

    @NotBlank
    private String venueAddress; // Event location

    private String addons; // Extra services
    private String deliveryTier; // Delivery option
    private java.math.BigDecimal deliveryFeeLkr; // Delivery fee
    private java.math.BigDecimal discountAmountLkr; // Discount amount
    private String promoCode; // Promotional code
    private String clientNotes; // Client's notes
}