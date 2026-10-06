package com.pixora.repository;

import com.pixora.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> { // Provides built-in CRUD operations
    //Find all Photos related to a booking
    List<Photo> findByBookingBookingId(Long bookingId);
    // Find all photos published in the portfolio
    List<Photo> findByIsPublishedPortfolioTrue();
    // Find all photos uploaded by a photographer
    List<Photo> findByPhotographerUserId(Long photographerId);
}