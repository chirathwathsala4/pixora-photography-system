package com.pixora.config;

import com.pixora.entity.User;
import com.pixora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final com.pixora.repository.PromoCodeRepository promoCodeRepository;

    @Override
    public void run(String... args) {
        // Ensure database ENUMs / columns accommodate PAID status and new feature columns
        try {
            jdbcTemplate.execute("ALTER TABLE bookings MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PENDING_ADMIN_APPROVAL'");
            jdbcTemplate.execute("ALTER TABLE payments MODIFY COLUMN payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING_APPROVAL'");
        } catch (Exception e) {
            log.warn("Could not alter bookings/payments table status columns: {}", e.getMessage());
        }

        // Add new columns to bookings table if not exist
        String[] bookingCols = {
            "ALTER TABLE bookings ADD COLUMN staff_status VARCHAR(50) NOT NULL DEFAULT 'UNSTAFFED'",
            "ALTER TABLE bookings ADD COLUMN addons VARCHAR(500) NULL",
            "ALTER TABLE bookings ADD COLUMN delivery_tier VARCHAR(50) DEFAULT 'STANDARD'",
            "ALTER TABLE bookings ADD COLUMN delivery_fee_lkr DECIMAL(12,2) DEFAULT 0",
            "ALTER TABLE bookings ADD COLUMN discount_amount_lkr DECIMAL(12,2) DEFAULT 0",
            "ALTER TABLE bookings ADD COLUMN promo_code VARCHAR(50) NULL",
            "ALTER TABLE bookings ADD COLUMN client_notes VARCHAR(1000) NULL"
        };
        for (String sql : bookingCols) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception ignored) {}
        }

        // Add is_favorite to photos table if not exist
        try {
            jdbcTemplate.execute("ALTER TABLE photos ADD COLUMN is_favorite BOOLEAN NOT NULL DEFAULT FALSE");
        } catch (Exception ignored) {}

        // Ensure reviews table columns
        try {
            jdbcTemplate.execute("ALTER TABLE reviews ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE reviews MODIFY COLUMN photographer_id BIGINT NULL");
        } catch (Exception ignored) {}

        // Seed default promo codes
        seedPromoCodes();

        // Ensure Master Admin account exists
        if (!userRepository.existsByEmail("admin@pixora.lk")) {
            User admin = User.builder()
                    .fullName("Master Admin")
                    .email("admin@pixora.lk")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(User.Role.ADMIN)
                    .accountStatus(User.AccountStatus.ACTIVE)
                    .build();
            userRepository.save(admin);
            log.info("Master Admin account created: admin@pixora.lk");
        } else {
            log.info("Admin account already exists, ensuring correct password...");
            User admin = userRepository.findByEmail("admin@pixora.lk").orElseThrow();
            // Re-encode to ensure it's correct (handles stale bcrypt from SQL seed)
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            userRepository.save(admin);
            log.info("Admin password refreshed.");
        }
    }

    private void seedPromoCodes() {
        if (promoCodeRepository.count() == 0) {
            promoCodeRepository.save(com.pixora.entity.PromoCode.builder()
                    .code("PIXORA10")
                    .discountPercent(10)
                    .maxDiscountLkr(new java.math.BigDecimal("10000"))
                    .minBookingAmountLkr(new java.math.BigDecimal("15000"))
                    .isActive(true)
                    .expiryDate(java.time.LocalDate.now().plusMonths(6))
                    .build());

            promoCodeRepository.save(com.pixora.entity.PromoCode.builder()
                    .code("BIRTHDAY15")
                    .discountPercent(15)
                    .maxDiscountLkr(new java.math.BigDecimal("15000"))
                    .minBookingAmountLkr(new java.math.BigDecimal("20000"))
                    .isActive(true)
                    .expiryDate(java.time.LocalDate.now().plusMonths(6))
                    .build());

            promoCodeRepository.save(com.pixora.entity.PromoCode.builder()
                    .code("GOLDEN20")
                    .discountPercent(20)
                    .maxDiscountLkr(new java.math.BigDecimal("25000"))
                    .minBookingAmountLkr(new java.math.BigDecimal("30000"))
                    .isActive(true)
                    .expiryDate(java.time.LocalDate.now().plusMonths(6))
                    .build());
            log.info("Default promo codes seeded: PIXORA10, BIRTHDAY15, GOLDEN20");
        }
    }
}