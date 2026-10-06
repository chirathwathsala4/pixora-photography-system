package com.pixora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Used to send review details
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    // Stores the review ID
    private Long reviewId;

    // Stores the booking ID
    private Long bookingId;

    // Stores the client ID
    private Long clientId;

    // Stores the client name
    private String clientName;

    // Stores the photographer ID
    private Long photographerId;

    // Stores the photographer name
    private String photographerName;

    // Stores the star rating
    private Integer starRating;

    // Stores the review comment
    private String reviewComment;

    // Stores the created date and time
    private LocalDateTime createdAt;

    // Returns the review ID as "id"
    @com.fasterxml.jackson.annotation.JsonProperty("id")
    public Long getId() { return reviewId; }

    // Returns the star rating as "rating"
    @com.fasterxml.jackson.annotation.JsonProperty("rating")
    public Integer getRating() { return starRating; }

    // Returns the review comment as "comment"
    @com.fasterxml.jackson.annotation.JsonProperty("comment")
    public String getComment() { return reviewComment; }
}