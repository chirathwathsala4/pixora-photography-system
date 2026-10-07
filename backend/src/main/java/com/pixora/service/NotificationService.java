package com.pixora.service;

import com.pixora.dto.NotificationResponse;
import com.pixora.entity.Notification;
import com.pixora.entity.User;
import com.pixora.repository.NotificationRepository;
import com.pixora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// Service implementation managing notification creation, retrieval, and status updates
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Create and save a new notification for a targeted recipient user
    @Transactional
    public void sendNotification(User user, String title, String message, String type, Long relatedBookingId) {
        // Build notification entity with provided details and default unread state
        Notification n = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .relatedBookingId(relatedBookingId)
                .isRead(false)
                .build();
        // Persist notification record in the database
        notificationRepository.save(n);
    }

    // Retrieve all notifications for a given user ordered newest first and mapped to DTOs
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // Mark a specific notification as read by its unique ID
    @Transactional
    public void markAsRead(Long notificationId) {
        // Fetch notification if present, set isRead flag to true, and update record
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    // Mark all notifications for a specific user as read
    @Transactional
    public void markAllReadForUser(Long userId) {
        // Fetch user notifications and iterate to update read status to true
        notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId).forEach(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    // Helper method to map Notification entity to NotificationResponse DTO
    private NotificationResponse toDto(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUser().getUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .relatedBookingId(n.getRelatedBookingId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}