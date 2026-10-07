package com.pixora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Full name is required")
    @com.fasterxml.jackson.annotation.JsonAlias({"name", "clientName", "photographerName"})
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Pattern(regexp = "^(|\\+?[0-9]{9,15})$", message = "Phone number must be a valid phone number (e.g. 10 digits).")
    private String phone;

    private String portfolioUrl;

    private String role;
}