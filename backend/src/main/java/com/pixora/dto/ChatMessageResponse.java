package com.pixora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Data Transfer Object for returning chat message details in response payloads.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    // Unique identifier for the chat message
    private Long messageId;

    // ID of the booking associated with this chat message
    private Long bookingId;

    // ID of the user who sent the message
    private Long senderId;

    // Full name or display name of the sender
    private String senderName;

    // Role of the sender (e.g., CLIENT, PHOTOGRAPHER, ADMIN)
    private String senderRole;

    // ID of the recipient user receiving the message
    private Long recipientId;
    
    // Full name or display name of the recipient
    private String recipientName;

    // Content/text of the chat message
    private String message;

    // Read status indicating whether the recipient has read the message
    private Boolean isRead;
    
    // Timestamp when the message was created
    private LocalDateTime createdAt;
}



