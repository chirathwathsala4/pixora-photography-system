package com.pixora.repository;

import com.pixora.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repository interface for managing database operations on ChatMessage entities
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Retrieve all chat messages for a specific booking ID ordered chronologically by creation time
    List<ChatMessage> findByBookingBookingIdOrderByCreatedAtAsc(Long bookingId);

    // Count total unread messages where the given user ID is the recipient
    long countByRecipientUserIdAndIsReadFalse(Long recipientId);

    // Delete all chat messages associated with a specific booking ID
    void deleteByBookingBookingId(Long bookingId);

    // Delete all chat messages where the specified user ID is either the sender or recipient
    void deleteBySenderUserIdOrRecipientUserId(Long senderId, Long recipientId);
}