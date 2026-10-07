package com.pixora.controller;

import com.pixora.dto.ChatMessageRequest;
import com.pixora.dto.ChatMessageResponse;
import com.pixora.entity.User;
import com.pixora.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing chat operations related to bookings.
 * Handles endpoints for fetching, sending, marking messages as read, and retrieving unread counts.
 */
@RestController
@RequestMapping({"/api/v1/chat", "/api/chat"})
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // Fetch all chat messages for a specific booking ID

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal User user) {
                // Retrieve booking messages using booking ID and authenticated user ID
        return ResponseEntity.ok(chatService.getBookingMessages(bookingId, user.getUserId()));
    }

    // Send a new chat message for a specific booking
    @PostMapping("/booking/{bookingId}")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long bookingId,
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal User user) {
                // Delegate message creation to chat service using request payload
        return ResponseEntity.ok(chatService.sendMessage(bookingId, user.getUserId(), request.getMessage()));
    }

    // Mark all unread messages in a booking chat as read
    @PutMapping("/booking/{bookingId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal User user) {
                // Update unread message status for the authenticated user
        chatService.markMessagesAsRead(bookingId, user.getUserId());
        // Return 200 OK response with no response body
        return ResponseEntity.ok().build();
    }
    

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("unreadCount", chatService.getUnreadCount(user.getUserId())));
    }
}
