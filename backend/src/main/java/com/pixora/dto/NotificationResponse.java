package com.pixora.dto;

import lombok.*;

import java.time.LocalDateTime;

// Data Transfer Object for returning notification details in response payloads
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    // Unique identifier for the notification
    private Long notificationId;

    // ID of the user who receives the notification
    private Long userId;

    // Short summary title of the notification
    private String title;

    // Detailed content message of the notification
    private String message;

    // Notification type category (e.g., BOOKING_UPDATE, CHAT_MESSAGE, PAYMENT)
    private String type;

    // Associated booking ID linked to this notification, if applicable
    private Long relatedBookingId;

    // Read status indicating whether the user has viewed the notification
    private Boolean isRead;

    // Timestamp when the notification was created
    private LocalDateTime createdAt;
}