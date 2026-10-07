package com.pixora.service;

import com.pixora.entity.PromoCode;
import com.pixora.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//This class contains business logic and should be managed by Spring
@Service
@RequiredArgsConstructor
public class PromoService {

    //This gives PromoService access to the database.FindAll()SavePayment()DeleteByID()
    private final PromoCodeRepository promoCodeRepository;

    //It validates a promo code and calculates the discount
    @Transactional(readOnly = true)
    public Map<String, Object> validatePromo(String code, BigDecimal bookingAmount) {
        Map<String, Object> result = new HashMap<>();
        //When we selecting the promo code remove spaces from selected one 
        if (code == null || code.trim().isEmpty()) {
            result.put("valid", false);
            result.put("message", "Promo code cannot be empty");
            return result;
        }
        
        //This searches the database for a promo code that,matches the code,ignores uppercase/lowercase,must be active
        PromoCode promo = promoCodeRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim())
                .orElse(null);
        
        //If no promo was found,Promo is invalid,Creates an appropriate error message
        if (promo == null) {
            result.put("valid", false);
            result.put("message", "Invalid or inactive promo code: " + code.trim());
            return result;
        }
        //Promo code can be expire only admin concept 
        if (promo.getExpiryDate() != null && promo.getExpiryDate().isBefore(LocalDate.now())) {
            result.put("valid", false);
            result.put("message", "This promo code has expired");
            return result;
        }
        //This checks whether the customer's booking amount meets the promo's minimum requirement.
        if (promo.getMinBookingAmountLkr() != null && bookingAmount != null &&
                bookingAmount.compareTo(promo.getMinBookingAmountLkr()) < 0) {
            result.put("valid", false);
            result.put("message", "Minimum booking spend for this promo is Rs. " + promo.getMinBookingAmountLkr());
            return result;
        }

        //We use BigDecimal for financial calculations to avoid floating-point precision problems
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (bookingAmount != null && bookingAmount.compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = bookingAmount.multiply(BigDecimal.valueOf(promo.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (promo.getMaxDiscountLkr() != null && discountAmount.compareTo(promo.getMaxDiscountLkr()) > 0) {
                discountAmount = promo.getMaxDiscountLkr();
            }
        }

        BigDecimal finalAmount = bookingAmount != null ? bookingAmount.subtract(discountAmount).max(BigDecimal.ZERO) : BigDecimal.ZERO;

        result.put("valid", true);
        result.put("code", promo.getCode());
        result.put("discountPercent", promo.getDiscountPercent());
        result.put("discountAmountLkr", discountAmount);
        result.put("finalAmountLkr", finalAmount);
        result.put("message", promo.getDiscountPercent() + "% discount applied successfully!");
        return result;
    }

    @Transactional(readOnly = true)
    public List<PromoCode> getAllPromos() {
        return promoCodeRepository.findAll();
    }

    @Transactional
    public PromoCode createPromo(PromoCode promoCode) {
        if (promoCodeRepository.existsByCodeIgnoreCase(promoCode.getCode())) {
            throw new RuntimeException("Promo code already exists: " + promoCode.getCode());
        }
        promoCode.setCode(promoCode.getCode().toUpperCase().trim());
        return promoCodeRepository.save(promoCode);
    }

    @Transactional
    public PromoCode togglePromo(Long promoId) {
        PromoCode p = promoCodeRepository.findById(promoId)
                .orElseThrow(() -> new RuntimeException("Promo not found"));
        p.setIsActive(!p.getIsActive());
        return promoCodeRepository.save(p);
    }

    @Transactional
    public void deletePromo(Long promoId) {
        promoCodeRepository.deleteById(promoId);
    }
}
