package com.pixora.service;

import com.pixora.config.JwtUtil;
import com.pixora.dto.AuthRequest;
import com.pixora.dto.AuthResponse;
import com.pixora.dto.RegisterRequest;
import com.pixora.entity.User;
import com.pixora.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Service implementation for managing authentication, registration, and user details loading
@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // Constructor injection for dependencies with Lazy annotation to resolve circular dependency on AuthenticationManager
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    // Load user details by email for Spring Security authentication provider
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Fetch user by email or throw exception if not found
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    // Process user registration request for clients or photographers
    public AuthResponse register(RegisterRequest request) {
        // Normalize email input to lowercase and trimmed string
        String email = request.getEmail().trim().toLowerCase();
        // Check if the provided email is already registered in the system
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("This email is already registered. Please sign in or use a different email.");
        }
        // Sanitize phone number input
        String phone = (request.getPhone() != null && !request.getPhone().isBlank())
                ? request.getPhone().trim() : null;

        // Determine user role and account status based on registration request
        boolean isPhotographer = "PHOTOGRAPHER".equalsIgnoreCase(request.getRole());
        User.Role role = isPhotographer ? User.Role.PHOTOGRAPHER : User.Role.CLIENT;
        User.AccountStatus status = isPhotographer ? User.AccountStatus.PENDING_APPROVAL : User.AccountStatus.ACTIVE;

        // Build user entity from registration request payload
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(phone)
                .portfolioUrl(request.getPortfolioUrl() != null && !request.getPortfolioUrl().isBlank()
                        ? request.getPortfolioUrl().trim() : null)
                .role(role)
                .accountStatus(status)
                .build();
        // Save new user entity to the database
        userRepository.save(user);

        // Generate JWT token only if account status is active
        String token = (status == User.AccountStatus.ACTIVE) ? jwtUtil.generateToken(user) : null;
        // Construct and return authentication response DTO
        return buildAuthResponse(user, token);
    }

    // Authenticate user credentials and generate authentication token
    public AuthResponse login(AuthRequest request) {
        // Authenticate credentials using Spring Security AuthenticationManager
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim().toLowerCase(), request.getPassword())
        );
        // Retrieve authenticated user record from database
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        // Generate JWT token for authorized session
        String token = jwtUtil.generateToken(user);
        // Return authentication response DTO with token and user details
        return buildAuthResponse(user, token);
    }

    // Submit a photographer application requiring admin approval
    public AuthResponse applyPhotographer(RegisterRequest request) {
        // Normalize email input
        String email = request.getEmail().trim().toLowerCase();
        // Ensure email address is unique
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("This email is already registered. Please sign in or use a different email.");
        }
        // Sanitize phone input
        String phone = (request.getPhone() != null && !request.getPhone().isBlank())
                ? request.getPhone().trim() : null;

        // Build photographer user entity with PENDING_APPROVAL status
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(phone)
                .portfolioUrl(request.getPortfolioUrl() != null && !request.getPortfolioUrl().isBlank()
                        ? request.getPortfolioUrl().trim() : null)
                .role(User.Role.PHOTOGRAPHER)
                .accountStatus(User.AccountStatus.PENDING_APPROVAL)
                .build();
        // Save pending photographer account
        userRepository.save(user);
        // Return auth response without JWT token since account approval is pending
        return buildAuthResponse(user, null);
    }

    // Helper method to map User entity and JWT token into AuthResponse DTO
    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .accountStatus(user.getAccountStatus().name())
                .build();
    }
}