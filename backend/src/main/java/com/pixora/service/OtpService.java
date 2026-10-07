package com.pixora.service;

import com.pixora.entity.User;
import com.pixora.pattern.singleton.OtpStore;
import com.pixora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Singleton pattern: every request shares the one OtpStore instance. */
    private final OtpStore otpStore = OtpStore.getInstance();
    private final SecureRandom random = new SecureRandom();

    public Map<String, Object> sendOtp(String rawPhone) {
        String phone = cleanPhone(rawPhone);
        if (phone.isEmpty()) {
            throw new RuntimeException("Phone number is required");
        }

        // Verify user exists by phone number
        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> u.getPhone() != null && cleanPhone(u.getPhone()).equals(phone))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No registered account found with phone number: " + rawPhone)));

        // Generate 6-digit OTP
        int codeInt = 100000 + random.nextInt(900000);
        String code = String.valueOf(codeInt);
        otpStore.saveOtp(phone, code, LocalDateTime.now().plusMinutes(10));

        log.info("OTP generated for phone {}: {}", phone, code);

        return Map.of(
                "success", true,
                "message", "Verification OTP sent to " + phone + ". Valid for 10 minutes.",
                "phoneNumber", phone,
                "debugOtp", code // Included for seamless test demonstration
        );
    }

    public Map<String, Object> verifyOtpAndResetPassword(String rawPhone, String otp, String newPassword) {
        String phone = cleanPhone(rawPhone);
        OtpStore.OtpEntry entry = otpStore.getOtp(phone);
        if (entry == null) {
            throw new RuntimeException("No OTP requested for this phone number or OTP has expired.");
        }

        if (entry.isExpired()) {
            otpStore.removeOtp(phone);
            throw new RuntimeException("OTP has expired. Please request a new code.");
        }

        if (!entry.getCode().equals(otp.trim())) {
            throw new RuntimeException("Invalid verification code. Please check and try again.");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters long.");
        }

        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> u.getPhone() != null && cleanPhone(u.getPhone()).equals(phone))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("User account not found.")));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Clear used OTP
        otpStore.removeOtp(phone);
        log.info("Password successfully reset via OTP for user ID {}", user.getUserId());

        return Map.of(
                "success", true,
                "message", "Password successfully reset! You can now log in with your new credentials."
        );
    }

    private String cleanPhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9+]", "").trim();
    }
}
