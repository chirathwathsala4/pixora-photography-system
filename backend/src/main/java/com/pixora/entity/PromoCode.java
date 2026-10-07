package com.pixora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

//This Java class represents a database table
@Entity
//This specifies the exact database table name.
@Table(name = "promo_codes")
//Automatically generte the setters & getters
@Data
//Doesn't create any parameters for the constrautor  
@NoArgsConstructor
@AllArgsConstructor
//This allows you to create a PromoCode object using the Builder Pattern
@Builder
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //Show the datbase column names 
    @Column(name = "promo_id")
    private Long promoId;

    //Explain the database column name rules,nullable can,t get a null value for that 
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    //promo must have a discount precentage 
    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    //defines the maximum discount amount in LKR,Number of digit,only 2 decimal points
    @Column(name = "max_discount_lkr", precision = 12, scale = 2)
    private BigDecimal maxDiscountLkr;

    @Column(name = "min_booking_amount_lkr", precision = 12, scale = 2)
    private BigDecimal minBookingAmountLkr;

    //The promo code is active and can be used.
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    //Booking package is expiryed, the promo should no longer be accepted by the validation logic.
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
