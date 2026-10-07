package com.pixora.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//BigDecimal for payment amounts because it provides accurate decimal calculations for financial values.
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    //stores the reference number
    private String transactionRef;
    
    @NotNull @DecimalMin("1.00")
    private BigDecimal amountPaidLkr;

    //Show the payment method select by customer,Selecting Card
    private String paymentMethod;
    private String cardholderName;
    private String cardNumber;
    private String expiryDate;
    private String cvv;
}
