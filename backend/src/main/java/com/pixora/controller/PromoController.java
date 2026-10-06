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

    private final PromoService promoService;

    @PostMapping("/public/promo/validate")
    public ResponseEntity<Map<String, Object>> validatePromo(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        Object amtObj = body.get("bookingAmount");
        BigDecimal amount = BigDecimal.ZERO;
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
