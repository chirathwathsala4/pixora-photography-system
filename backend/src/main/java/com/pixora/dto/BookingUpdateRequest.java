package com.pixora.dto; // DTO package

import jakarta.validation.constraints.FutureOrPresent; // Date cannot be in the past
import jakarta.validation.constraints.NotBlank; // String cannot be empty
import jakarta.validation.constraints.NotNull; // Value is required
import lombok.Data; // Generates getters and setters

import java.time.LocalDate; // Date
import java.time.LocalTime; // Time

@Data // Generates getters, setters, etc.
public class BookingUpdateRequest {

    @NotNull
    @FutureOrPresent
    private LocalDate eventDate; // Updated event date

    @NotNull
    private LocalTime eventTime; // Updated event time

    @NotBlank
    private String venueAddress; // Updated event location

    private String clientNotes; // Updated client notes
}