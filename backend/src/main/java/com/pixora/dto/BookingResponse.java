package com.pixora.dto; // DTO package

import lombok.*; // Lombok annotations
import java.math.BigDecimal; // For money values
import java.time.LocalDate; // Date
import java.time.LocalTime; // Time

@Data // Generates getters and setters
@Builder // Allows creating objects using builder pattern
@NoArgsConstructor // Creates empty constructor
@AllArgsConstructor // Creates constructor with all fields

public class BookingResponse {

    private Long bookingId; // Booking ID

    private Long clientId; // Client ID
    private String clientName; // Client name

    private Long photographerId; // Photographer ID
    private String photographerName; // Photographer name

    private Long packageId; // Package ID
    private String packageName; // Package name

    private BigDecimal priceLkr; // Package price

    private LocalDate eventDate; // Event date
    private LocalTime eventTime; // Event time

    private String venueAddress; // Event location

    private BigDecimal totalAmountLkr; // Final booking amount

    private String status; // Booking status
    private String staffStatus; // Staff processing status
    private String paymentStatus; // Payment status

    private String addons; // Additional services
    private String deliveryTier; // Delivery option
    private BigDecimal deliveryFeeLkr; // Delivery fee
    private BigDecimal discountAmountLkr; // Discount amount
    private String promoCode; // Promotional code
    private String clientNotes; // Client's notes
}