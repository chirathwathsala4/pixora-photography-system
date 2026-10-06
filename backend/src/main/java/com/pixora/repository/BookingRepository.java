package com.pixora.repository; // Repository package

import com.pixora.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository; // Provides CRUD operations
import org.springframework.data.jpa.repository.Query; // For custom queries
import org.springframework.data.repository.query.Param; // Maps parameters to query
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository // Marks this interface as a repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find all bookings made by a specific client
    List<Booking> findByClientUserId(Long clientId);

    // Find all bookings assigned to a specific photographer
    List<Booking> findByPhotographerUserId(Long photographerId);

    // Find photographer's bookings on a specific date
    // Excludes cancelled bookings
    @Query("SELECT b FROM Booking b WHERE b.photographer.userId = :photographerId " +
           "AND b.eventDate = :date AND b.status != com.pixora.entity.Booking$BookingStatus.CANCELLED")
    List<Booking> findByPhotographerAndDate(
            @Param("photographerId") Long photographerId,
            @Param("date") LocalDate date);

    // Get photographer IDs who are already booked on a date
    // Excludes cancelled bookings and bookings without a photographer
    @Query("SELECT b.photographer.userId FROM Booking b WHERE b.eventDate = :date " +
           "AND b.status != com.pixora.entity.Booking$BookingStatus.CANCELLED " +
           "AND b.photographer IS NOT NULL")
    List<Long> findBookedPhotographerIdsByDate(@Param("date") LocalDate date);
}