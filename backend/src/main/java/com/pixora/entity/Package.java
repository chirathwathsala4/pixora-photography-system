package com.pixora.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

// JPA Entity representing the packages table in the database
@Entity
@Table(name = "packages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Package {

    // Primary key with auto-increment strategy mapped to package_id column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "package_id")
    private Long packageId;

    // Display name of the photography package (max 200 characters)
    @Column(name = "package_name", nullable = false, length = 200)
    private String packageName;

    // Package price in LKR stored as a high-precision decimal (12 digits, 2 decimal places)
    @Column(name = "price_lkr", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceLkr;

    // Detailed description of inclusions and features stored as TEXT in the database
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Flag indicating whether the package is currently available or active
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    // One-to-Many relationship mapping to associated bookings, excluded from toString and JSON serialization
    @OneToMany(mappedBy = "pkg", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<Booking> bookings;
}