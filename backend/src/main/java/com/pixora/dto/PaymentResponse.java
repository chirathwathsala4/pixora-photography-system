package com.pixora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

//automatically generates getters, setters and other common methods
@Data
//implements the Builder Pattern and allows us to create the PaymentResponse object step by step.
@Builder
@NoArgsConstructor
@AllArgsConstructor
//PaymentResponse is a DTO used to transfer payment information from the backend to the frontend
public class PaymentResponse {
    private Long paymentId;
    private Long bookingId;
    private String transactionRef;
    private BigDecimal amountPaidLkr;
    private String paymentStatus;
    private String clientName;
    private String packageName;
}
