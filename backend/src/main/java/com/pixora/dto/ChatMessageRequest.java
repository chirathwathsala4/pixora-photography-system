package com.pixora.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Data Transfer Object for sending a chat message request payload
@Data
public class ChatMessageRequest {
    // Ensure the chat message field is not null, empty, or whitespace-only
@NotBlank
private String message;
}