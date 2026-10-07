package com.pixora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// JPA Entity representing the notifications table in the database
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    // Primary key with auto-increment strategy mapped to notification_id column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    // Many-to-One relationship mapping to the user receiving the notification
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Short summary title of the notification (max 255 characters)
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    // Detailed body message of the notification stored as TEXT type in the database
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    // Category or type identifier for the notification (max 50 characters)
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    // Optional reference to a related booking ID associated with this notification
    @Column(name = "related_booking_id")
    private Long relatedBookingId;

    // Read status flag indicating if the user has viewed the notification (defaults to false)
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    // Timestamp recorded when the notification was generated (defaults to current time)
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}