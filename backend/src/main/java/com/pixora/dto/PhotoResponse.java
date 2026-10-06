package com.pixora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

 // DTO for returing photo details to the client
public class PhotoResponse {
    // Unique photo ID
    private Long photoId;
    //Related bokking ID.
    private Long bookingId;
    // Photographer ID
    private Long photographerId;
    //Photogrpher Name
    private String photographerName;
    // photo URL
    private String photoUrl;
    //Portfolio publication status.
    private Boolean isPublishedPortfolio;
    //Favorite status of the Photo.
    private Boolean isFavorite;
}