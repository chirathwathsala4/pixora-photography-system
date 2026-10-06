package com.pixora.controller;

import com.pixora.dto.*;
import com.pixora.entity.Booking;
import com.pixora.entity.Payment;
import com.pixora.entity.User;
import com.pixora.exception.ResourceNotFoundException;
import com.pixora.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PhotoService photoService;
    private final ReviewService reviewService;
    private final PdfReceiptService pdfReceiptService;
    private final UserService userService;
    private final NotificationService notificationService;

    // Bookings
    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookingService.getClientBookings(user.getUserId()));
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(@AuthenticationPrincipal User user,
                                                         @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(request, user.getUserId()));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @PutMapping("/bookings/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id,
                                           @AuthenticationPrincipal User user) {
        Booking booking = bookingService.findBooking(id);
        if (!booking.getClient().getUserId().equals(user.getUserId())) {
            return ResponseEntity.status(403).build();
        }
        if (booking.getStatus() == Booking.BookingStatus.PENDING_ADMIN_APPROVAL) {
            bookingService.cancelPendingBooking(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }

    // Payments
    @GetMapping("/bookings/{id}/payment")
    public ResponseEntity<?> getPayment(@PathVariable Long id) {
        Optional<PaymentResponse> payment = paymentService.getPaymentByBooking(id);
        return payment.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/bookings/{id}/payment")
    public ResponseEntity<PaymentResponse> submitPayment(@PathVariable Long id,
                                                         @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.submitPayment(id, request));
    }

    // PDF Receipt
    @GetMapping("/bookings/{id}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id,
                                                  @AuthenticationPrincipal User user) {
        try {
            Booking booking = bookingService.findBooking(id);
            Optional<PaymentResponse> payment = paymentService.getPaymentByBooking(id);
            if (payment.isEmpty()) {
                return ResponseEntity.status(403).build();
            }
            String status = payment.get().getPaymentStatus();
            if (!"APPROVED".equals(status) && !"PAID".equals(status)) {
                return ResponseEntity.status(403).build();
            }
            byte[] pdf = pdfReceiptService.generateReceipt(booking);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=Pixora-Receipt-" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Edit Booking
    @PutMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> updateBooking(@PathVariable Long id,
                                                         @AuthenticationPrincipal User user,
                                                         @Valid @RequestBody com.pixora.dto.BookingUpdateRequest request) {
        return ResponseEntity.ok(bookingService.updateClientBooking(id, user.getUserId(), request));
    }

    // Gallery
    @GetMapping("/bookings/{id}/photos")
    public ResponseEntity<List<PhotoResponse>> getGallery(@PathVariable Long id) {
        return ResponseEntity.ok(photoService.getPhotosByBooking(id));
    }

    // Toggle Photo Favorite
    @PutMapping({"/photos/{photoId}/favorite", "/photos/{photoId}/toggle-favorite"})
    public ResponseEntity<PhotoResponse> togglePhotoFavorite(@PathVariable Long photoId,
                                                             @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(photoService.toggleFavorite(photoId, user.getUserId()));
    }

    // Reviews
    @PostMapping("/bookings/{id}/review")
    public ResponseEntity<ReviewResponse> submitReview(@PathVariable Long id,
                                                       @AuthenticationPrincipal User user,
                                                       @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.submitReview(id, user.getUserId(), request));
    }

    @GetMapping("/bookings/{id}/review")
    public ResponseEntity<?> getReview(@PathVariable Long id) {
        Optional<ReviewResponse> review = reviewService.getReviewByBooking(id);
        return review.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long reviewId,
                                                       @AuthenticationPrincipal User user,
                                                       @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, user.getUserId(), request));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId,
                                             @AuthenticationPrincipal User user) {
        reviewService.deleteReviewByClient(reviewId, user.getUserId());
        return ResponseEntity.noContent().build();
    }

    // Notifications
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getUserNotifications(user.getUserId()));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal User user) {
        notificationService.markAllReadForUser(user.getUserId());
        return ResponseEntity.ok().build();
    }

    //Shows Available photographers by date
    @GetMapping("/photographers/available")
    public ResponseEntity<?> getAvailablePhotographers(@RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return ResponseEntity.ok(bookingService.getAvailablePhotographers(localDate));
    }
}