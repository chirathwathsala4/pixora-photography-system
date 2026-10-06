package com.pixora.entity; // Entity package

import com.fasterxml.jackson.annotation.JsonIgnore; // Prevents field from being returned in JSON
import jakarta.persistence.*; // JPA database annotations
import lombok.*; // Lombok annotations

import java.math.BigDecimal; // For money values
import java.time.LocalDate; // Date
import java.time.LocalTime; // Time
import java.util.List; // List of related records

@Entity // Marks this class as a database entity
@Table(name = "bookings") // Maps to the bookings table
@Data // Generates getters and setters
@NoArgsConstructor // Creates empty constructor
@AllArgsConstructor // Creates constructor with all fields
@Builder // Allows builder pattern
public class Booking {

    @Id // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates ID
    @Column(name = "booking_id")
    private Long bookingId; // Booking ID

    @ManyToOne(fetch = FetchType.EAGER) // Many bookings can belong to one client
    @JoinColumn(name = "client_id", nullable = false) // Foreign key to User
    private User client; // Client who made booking

    @ManyToOne(fetch = FetchType.EAGER) // Many bookings can have one photographer
    @JoinColumn(name = "photographer_id")
    private User photographer; // Photographer for booking

    @ManyToOne(fetch = FetchType.EAGER) // Many bookings can use one package
    @JoinColumn(name = "package_id", nullable = false) // Foreign key to Package
    private Package pkg; // Selected photography package

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate; // Event date

    @Column(name = "event_time", nullable = false)
    private LocalTime eventTime; // Event time

    @Column(name = "venue_address", nullable = false, length = 500)
    private String venueAddress; // Event location

    @Column(name = "total_amount_lkr", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmountLkr; // Total booking amount

    @Column(name = "addons", length = 500)
    private String addons; // Additional services

    @Column(name = "delivery_tier", length = 50)
    @Builder.Default
    private String deliveryTier = "STANDARD"; // Default delivery option

    @Column(name = "delivery_fee_lkr", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFeeLkr = BigDecimal.ZERO; // Delivery fee

    @Column(name = "discount_amount_lkr", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmountLkr = BigDecimal.ZERO; // Discount amount

    @Column(name = "promo_code", length = 50)
    private String promoCode; // Promotional code

    @Column(name = "client_notes", length = 1000)
    private String clientNotes; // Client's additional notes

    @Enumerated(EnumType.STRING) // Stores enum as text in database
    @Column(name = "status", nullable = false)
    private BookingStatus status; // Booking status

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_status", nullable = false)
    @Builder.Default
    private StaffStatus staffStatus = StaffStatus.UNSTAFFED; // Staff status

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude // Prevents recursive toString
    @JsonIgnore // Prevents sending payments in JSON
    private List<Payment> payments; // Payments for this booking

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<Photo> photos; // Photos related to booking

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private Review review; // Review for this booking

    // Possible booking statuses
    public enum BookingStatus {
        PENDING_ADMIN_APPROVAL, CONFIRMED, PAID, CANCELLED, COMPLETED
    }

    // Possible staff statuses
    public enum StaffStatus {
        UNSTAFFED, PENDING_ACCEPTANCE, STAFFED, DECLINED
    }
}