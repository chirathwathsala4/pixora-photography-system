package com.pixora.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private String transactionRef;

    @NotNull @DecimalMin("1.00")
    private BigDecimal amountPaidLkr;

    private String paymentMethod;
    private String cardholderName;
    private String cardNumber;
    private String expiryDate;
    private String cvv;
}
