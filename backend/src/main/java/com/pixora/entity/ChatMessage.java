package com.pixora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// JPA Entity representing the chat_messages table in the database
@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    // Primary key with auto-increment strategy mapped to message_id column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    // Many-to-One relationship mapping to the associated booking record
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // Many-to-One relationship mapping to the user who sent the message
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // Many-to-One relationship mapping to the user receiving the message
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    // Text content of the chat message stored as TEXT type in the database
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    // Read status flag indicating if the recipient has viewed the message (defaults to false)
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    // Timestamp recorded when the message was created (defaults to current time)
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}