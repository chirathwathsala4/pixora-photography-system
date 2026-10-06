package com.pixora.service;

import com.pixora.dto.ChatMessageResponse;
import com.pixora.entity.Booking;
import com.pixora.entity.ChatMessage;
import com.pixora.entity.User;
import com.pixora.repository.BookingRepository;
import com.pixora.repository.ChatMessageRepository;
import com.pixora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// Service implementation handling chat business logic, message dispatching, and notifications
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // Send a new message, infer recipient role, persist message, and trigger notification
    @Transactional
    public ChatMessageResponse sendMessage(Long bookingId, Long senderId, String messageText) {
        // Fetch booking entity by ID or throw exception if not found
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        // Fetch sender user entity by ID or throw exception if not found
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Determine recipient user based on sender role and booking assignment
        User recipient;
        if (sender.getRole() == User.Role.CLIENT) {
            if (booking.getPhotographer() != null) {
                recipient = booking.getPhotographer();
            } else {
                // If unstaffed, send to admin
                recipient = userRepository.findByEmail("admin@pixora.lk")
                        .orElse(booking.getClient());
            }
        } else if (sender.getRole() == User.Role.PHOTOGRAPHER) {
            recipient = booking.getClient();
        } else {
            // Admin sending: if to client or photographer
            recipient = booking.getClient();
        }

        // Build new chat message entity
        ChatMessage chatMessage = ChatMessage.builder()
                .booking(booking)
                .sender(sender)
                .recipient(recipient)
                .message(messageText.trim())
                .isRead(false)
                .build();

        // Save message record in database
        ChatMessage saved = chatMessageRepository.save(chatMessage);

        // Send in-app notification to recipient
        notificationService.sendNotification(
                recipient,
                "New message from " + sender.getFullName(),
                messageText.length() > 50 ? messageText.substring(0, 47) + "..." : messageText,
                "CHAT",
                booking.getBookingId()
        );

        // Convert and return saved entity as DTO response
        return toDto(saved);
    }

    // Retrieve all chat messages for a booking after verifying user authorization
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getBookingMessages(Long bookingId, Long userId) {
        // Fetch booking entity or throw exception if missing
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Fetch user making the request and verify access rights
        User user = userRepository.findById(userId).orElseThrow();
        boolean isAuthorized = user.getRole() == User.Role.ADMIN ||
                booking.getClient().getUserId().equals(userId) ||
                (booking.getPhotographer() != null && booking.getPhotographer().getUserId().equals(userId));

        // Throw exception if user is not authorized to view messages
        if (!isAuthorized) {
            throw new RuntimeException("Unauthorized to view messages for this booking");
        }

        // Fetch chronologically ordered messages and map to DTO list
        return chatMessageRepository.findByBookingBookingIdOrderByCreatedAtAsc(bookingId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Mark unread messages in a booking chat as read for the current recipient
    @Transactional
    public void markMessagesAsRead(Long bookingId, Long userId) {
        // Retrieve all chat messages for the specified booking
        List<ChatMessage> messages = chatMessageRepository.findByBookingBookingIdOrderByCreatedAtAsc(bookingId);
        // Iterate through messages and mark unread items belonging to current recipient
        for (ChatMessage m : messages) {
            if (m.getRecipient().getUserId().equals(userId) && !m.getIsRead()) {
                m.setIsRead(true);
                chatMessageRepository.save(m);
            }
        }
    }

    // Get total count of unread messages for a specific recipient user
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return chatMessageRepository.countByRecipientUserIdAndIsReadFalse(userId);
    }

    // Helper method to convert ChatMessage entity to ChatMessageResponse DTO
    private ChatMessageResponse toDto(ChatMessage m) {
        return ChatMessageResponse.builder()
                .messageId(m.getMessageId())
                .bookingId(m.getBooking().getBookingId())
                .senderId(m.getSender().getUserId())
                .senderName(m.getSender().getFullName())
                .senderRole(m.getSender().getRole().name())
                .recipientId(m.getRecipient().getUserId())
                .recipientName(m.getRecipient().getFullName())
                .message(m.getMessage())
                .isRead(m.getIsRead())
                .createdAt(m.getCreatedAt())
                .build();
    }
}