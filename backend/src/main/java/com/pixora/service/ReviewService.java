package com.pixora.service;

import com.pixora.dto.ReviewRequest;
import com.pixora.dto.ReviewResponse;
import com.pixora.entity.Booking;
import com.pixora.entity.Review;
import com.pixora.exception.ResourceNotFoundException;
import com.pixora.repository.BookingRepository;
import com.pixora.repository.ReviewRepository;
import com.pixora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Handles review and feedback functions
@Service
@RequiredArgsConstructor
public class ReviewService {

    // Used to access review data
    private final ReviewRepository reviewRepository;

    // Used to access booking data
    private final BookingRepository bookingRepository;

    // Used to access user data
    private final UserRepository userRepository;

    // Adds a new review for a completed booking
    @Transactional
    public ReviewResponse submitReview(Long bookingId, Long clientId, ReviewRequest request) {

        // Gets the booking ID
        Long targetBookingId = (bookingId != null) ? bookingId : request.getBookingId();

        // Checks if booking ID is available
        if (targetBookingId == null) {
            throw new RuntimeException("Booking ID is required to submit a review.");
        }

        // Finds the booking
        Booking booking = bookingRepository.findById(targetBookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + targetBookingId));

        // Allows reviews only for completed bookings
        if (booking.getStatus() != Booking.BookingStatus.COMPLETED) {
            throw new RuntimeException("Reviews can only be submitted for completed celebrations.");
        }

        // Checks if the booking belongs to the client
        if (!booking.getClient().getUserId().equals(clientId)) {
            throw new RuntimeException("You can only review your own bookings.");
        }

        // Prevents adding another review for the same booking
        if (reviewRepository.findByBookingBookingId(targetBookingId).isPresent()) {
            throw new RuntimeException("Review already submitted for this booking.");
        }

        // Creates the new review
        Review review = Review.builder()
                .booking(booking)
                .client(booking.getClient())
                .photographer(booking.getPhotographer())
                .starRating(request.getStarRating())
                .reviewComment(request.getReviewComment())
                .createdAt(LocalDateTime.now())
                .build();

        // Saves the review
        Review saved = reviewRepository.save(review);

        // Connects the review with the booking
        booking.setReview(saved);
        bookingRepository.save(booking);

        // Returns the saved review
        return toResponse(saved);
    }

    // Updates a review added by the client
    @Transactional
    public ReviewResponse updateReview(Long reviewId, Long clientId, ReviewRequest request) {

        // Finds the review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // Checks if the review belongs to the client
        if (!review.getClient().getUserId().equals(clientId)) {
            throw new RuntimeException("You can only edit your own reviews.");
        }

        // Updates the rating and comment
        review.setStarRating(request.getStarRating());
        review.setReviewComment(request.getReviewComment());

        // Saves and returns the updated review
        return toResponse(reviewRepository.save(review));
    }

    // Updates a review using the booking ID
    @Transactional
    public ReviewResponse updateReviewByBooking(Long bookingId, Long clientId, ReviewRequest request) {

        // Finds the review using the booking ID
        Review review = reviewRepository.findByBookingBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No review found for booking #" + bookingId));

        // Checks if the review belongs to the client
        if (!review.getClient().getUserId().equals(clientId)) {
            throw new RuntimeException("You can only edit your own reviews.");
        }

        // Updates the rating and comment
        review.setStarRating(request.getStarRating());
        review.setReviewComment(request.getReviewComment());

        // Saves and returns the updated review
        return toResponse(reviewRepository.save(review));
    }

    // Updates a review by admin
    @Transactional
    public ReviewResponse updateReviewByAdmin(Long reviewId, ReviewRequest request) {

        // Finds the review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // Updates the rating if it is provided
        if (request.getStarRating() != null) {
            review.setStarRating(request.getStarRating());
        }

        // Updates the comment if it is provided
        if (request.getReviewComment() != null) {
            review.setReviewComment(request.getReviewComment());
        }

        // Saves and returns the updated review
        return toResponse(reviewRepository.save(review));
    }

    // Deletes a review added by the client
    @Transactional
    public void deleteReviewByClient(Long reviewId, Long clientId) {

        // Finds the review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // Checks if the review belongs to the client
        if (!review.getClient().getUserId().equals(clientId)) {
            throw new RuntimeException("You can only delete your own reviews.");
        }

        // Gets the related booking
        Booking booking = review.getBooking();

        // Removes the review from the booking
        if (booking != null) {
            booking.setReview(null);
            bookingRepository.save(booking);
        }

        // Deletes the review
        reviewRepository.delete(review);
        reviewRepository.flush();
    }

    // Deletes a client's review using the booking ID
    @Transactional
    public void deleteReviewByBookingAndClient(Long bookingId, Long clientId) {

        // Finds the review using the booking ID
        Review review = reviewRepository.findByBookingBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No review found for booking #" + bookingId));

        // Checks if the review belongs to the client
        if (!review.getClient().getUserId().equals(clientId)) {
            throw new RuntimeException("You can only delete your own reviews.");
        }

        // Gets the related booking
        Booking booking = review.getBooking();

        // Removes the review from the booking
        if (booking != null) {
            booking.setReview(null);
            bookingRepository.save(booking);
        }

        // Deletes the review
        reviewRepository.delete(review);
        reviewRepository.flush();
    }

    // Deletes a review by admin
    @Transactional
    public void deleteReviewByAdmin(Long reviewId) {

        // Finds the review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // Gets the related booking
        Booking booking = review.getBooking();

        // Removes the review from the booking
        if (booking != null) {
            booking.setReview(null);
            bookingRepository.save(booking);
        }

        // Deletes the review
        reviewRepository.delete(review);
        reviewRepository.flush();
    }

    // Gets a review using the booking ID
    @Transactional(readOnly = true)
    public Optional<ReviewResponse> getReviewByBooking(Long bookingId) {
        return reviewRepository.findByBookingBookingId(bookingId).map(this::toResponse);
    }

    // Gets a review using the review ID
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
    }

    // Gets all reviews added by a client
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByClient(Long clientId) {
        return reviewRepository.findByClientUserId(clientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // Gets all reviews for a photographer
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByPhotographer(Long photographerId) {
        return reviewRepository.findByPhotographerUserId(photographerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // Gets all reviews
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    // Converts Review entity into ReviewResponse
    public ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .reviewId(r.getReviewId())
                .bookingId(r.getBooking() != null ? r.getBooking().getBookingId() : null)
                .clientId(r.getClient() != null ? r.getClient().getUserId() : null)
                .clientName(r.getClient() != null ? r.getClient().getFullName() : "Client")
                .photographerId(r.getPhotographer() != null ? r.getPhotographer().getUserId() : null)
                .photographerName(r.getPhotographer() != null ? r.getPhotographer().getFullName() : "Pixora Team")
                .starRating(r.getStarRating())
                .reviewComment(r.getReviewComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}