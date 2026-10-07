package com.pixora.controller;

import com.pixora.entity.PromoCode;
import com.pixora.service.PromoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PromoController {

    //Controller is depending on promo service
    private final PromoService promoService;

    //Validate the promo code 
    @PostMapping("/public/promo/validate")
    //Recieve the request to the fornt end
    public ResponseEntity<Map<String, Object>> validatePromo(@RequestBody Map<String, Object> body) {
        //This gets the code value from the request.
        String code = (String) body.get("code");
        //This gets the booking amount from the request.
        Object amtObj = body.get("bookingAmount");
        //Initially, the amount is set to zero.
        BigDecimal amount = BigDecimal.ZERO;
        //This checks whether the received booking amount is a number & Convert to Big Decimal
        if (amtObj instanceof Number) {
            amount = BigDecimal.valueOf(((Number) amtObj).doubleValue());
        }
        return ResponseEntity.ok(promoService.validatePromo(code, amount));
    }

    @GetMapping("/admin/promos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PromoCode>> getAllPromos() {
        return ResponseEntity.ok(promoService.getAllPromos());
    }

    @PostMapping("/admin/promos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCode> createPromo(@RequestBody PromoCode promoCode) {
        return ResponseEntity.ok(promoService.createPromo(promoCode));
    }

    @PutMapping("/admin/promos/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCode> togglePromo(@PathVariable Long id) {
        return ResponseEntity.ok(promoService.togglePromo(id));
    }

    @DeleteMapping("/admin/promos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePromo(@PathVariable Long id) {
        promoService.deletePromo(id);
        return ResponseEntity.noContent().build();
    }
}
//The promo code is used to provide discounts to customers when they make a photography booking.
//The customer enters the promo code during checkout, and the system validates the code and
//booking amount before applying the applicable discount. 
