package com.pixora.repository;

import com.pixora.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repository interface for managing database operations on Notification entities
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Retrieve all notifications for a specific user ID ordered newest first by creation time
    List<Notification> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    // Count total unread notifications for a specific user ID
    long countByUserUserIdAndIsReadFalse(Long userId);

    // Delete all notifications belonging to a specific user ID
    void deleteByUserUserId(Long userId);
}