package com.pixora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Represents a review in the database
@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    // Stores the unique review ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    // Connects the review with one booking
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    // Stores the client who added the review
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    // Stores the photographer who received the review
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "photographer_id", nullable = true)
    private User photographer;

    // Stores the star rating
    @Column(name = "star_rating", nullable = false)
    private Integer starRating;

    // Stores the feedback comment
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    // Stores the date and time when the review was created
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}