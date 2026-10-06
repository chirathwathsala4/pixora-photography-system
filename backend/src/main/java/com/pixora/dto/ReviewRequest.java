package com.pixora.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

// DTO class used to receive review and feedback data from the client
@Data
public class ReviewRequest {

    // Stores the booking ID related to the review
    private Long bookingId;

    // Ensures that the star rating is provided
    @NotNull(message = "Star rating is required")

    // Ensures that the minimum star rating is 1
    @Min(value = 1, message = "Star rating must be at least 1")

    // Ensures that the maximum star rating is 5
    @Max(value = 5, message = "Star rating cannot exceed 5")

    // Accepts "rating" or "stars" as JSON field names for starRating
    @com.fasterxml.jackson.annotation.JsonAlias({"rating", "stars"})
    private Integer starRating;

    // Ensures that the feedback is not empty
    @NotBlank(message = "Feedback must be between 3 and 1000 characters long.")

    // Ensures that the feedback contains between 3 and 1000 characters
    @Size(min = 3, max = 1000, message = "Feedback must be between 3 and 1000 characters long.")

    // Accepts "comment" or "feedback" as JSON field names for reviewComment
    @com.fasterxml.jackson.annotation.JsonAlias({"comment", "feedback"})
    private String reviewComment;
}