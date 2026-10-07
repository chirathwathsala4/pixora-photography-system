package com.pixora.service;

import com.pixora.dto.UserDto;
import com.pixora.entity.User;
import com.pixora.exception.ResourceNotFoundException;
import com.pixora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final com.pixora.repository.BookingRepository bookingRepository;
    private final com.pixora.repository.PaymentRepository paymentRepository;
    private final com.pixora.repository.PhotoRepository photoRepository;
    private final com.pixora.repository.ReviewRepository reviewRepository;
    private final com.pixora.repository.NotificationRepository notificationRepository;

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<UserDto> getClients() {
        return userRepository.findByRole(User.Role.CLIENT)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public UserDto getUserById(Long id) {
        return toDto(findUser(id));
    }

    //List users who come under the role of photographers
    public List<UserDto> getPhotographers() {
        return userRepository.findByRole(User.Role.PHOTOGRAPHER)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    //List users who come under the role of photographer with account status = "active"
    public List<UserDto> getActivePhotographers() {
        return userRepository.findByRoleAndAccountStatus(User.Role.PHOTOGRAPHER, User.AccountStatus.ACTIVE)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    //List users who come under the role of photographer with account status = "pending"
    public List<UserDto> getPendingPhotographers() {
        return userRepository.findByRoleAndAccountStatus(User.Role.PHOTOGRAPHER, User.AccountStatus.PENDING_APPROVAL)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    //Approve photographer
    public UserDto approvePhotographer(Long userId) {
        User user = findUser(userId);
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        return toDto(userRepository.save(user));
    }

    //Reject photographer
    public UserDto rejectPhotographer(Long userId) {
        User user = findUser(userId);
        user.setAccountStatus(User.AccountStatus.REJECTED);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findUser(userId);

        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Master Admin accounts cannot be deleted");
        }

        if (user.getRole() == User.Role.CLIENT) {
            // Cascade delete client's bookings, payments, photos, reviews
            List<com.pixora.entity.Booking> clientBookings = bookingRepository.findByClientUserId(userId);
            for (com.pixora.entity.Booking b : clientBookings) {
                reviewRepository.findByBookingBookingId(b.getBookingId()).ifPresent(reviewRepository::delete);
                photoRepository.deleteAll(photoRepository.findByBookingBookingId(b.getBookingId()));
                paymentRepository.findByBookingBookingId(b.getBookingId()).ifPresent(paymentRepository::delete);
                bookingRepository.delete(b);
            }
            reviewRepository.deleteAll(reviewRepository.findByClientUserId(userId));
        } else if (user.getRole() == User.Role.PHOTOGRAPHER) {
            // Unassign photographer from bookings safely without deleting client bookings
            List<com.pixora.entity.Booking> assignedBookings = bookingRepository.findByPhotographerUserId(userId);
            for (com.pixora.entity.Booking b : assignedBookings) {
                b.setPhotographer(null);
                b.setStaffStatus(com.pixora.entity.Booking.StaffStatus.UNSTAFFED);
                bookingRepository.save(b);
            }
            // Delete photos and reviews associated with this photographer
            photoRepository.deleteAll(photoRepository.findByPhotographerUserId(userId));
            reviewRepository.deleteAll(reviewRepository.findByPhotographerUserId(userId));
        }

        // Delete user notifications
        notificationRepository.deleteByUserUserId(userId);

        userRepository.delete(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public UserDto toDto(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .portfolioUrl(user.getPortfolioUrl())
                .accountStatus(user.getAccountStatus().name())
                .build();
    }
}