package com.pixora.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Photo {
    //Uniqu identifier for the photo
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Long photoId;
     
    //Booking associated the photo
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // Photographer who uploaded the photo
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uploaded_by_photographer_id", nullable = false)
    private User photographer;

    // URL of the uploaded photo
    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;

    //Indicates whether the photo is published in the portfolio
    @Column(name = "is_published_portfolio", nullable = false)
    private Boolean isPublishedPortfolio;

    // Indicates whether the photo is marked as a favorite
    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private Boolean isFavorite = false;
}