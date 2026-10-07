package com.pixora.repository;

import com.pixora.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Used to access review data from the database
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Finds a review using the booking ID
    Optional<Review> findByBookingBookingId(Long bookingId);

    // Finds all reviews for a photographer
    List<Review> findByPhotographerUserId(Long photographerId);

    // Finds all reviews added by a client
    List<Review> findByClientUserId(Long clientId);
}