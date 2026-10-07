package com.pixora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    //Defines the primary key of the payments table.
    @Id
    //The database automatically generates the payment ID
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //Stores the unique ID of each payment
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.EAGER)
    //foreign key column used to connect the payments table with the bookings table.
    @JoinColumn(name = "booking_id", nullable = false)
    //This stores the Booking object associated with the payment.
    private Booking booking;

    //This defines the database column for the transaction reference,Tranaction provide,characters 200.
    @Column(name = "transaction_ref", nullable = false, length = 200)
    private String transactionRef;
    
    //Amount may be use LKR,Amount cannot be not null,Amount use 12 digits,2 decimals points 
    @Column(name = "amount_paid_lkr", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaidLkr;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    public enum PaymentStatus {
        PENDING_APPROVAL, APPROVED, REJECTED, PAID
    }
}
