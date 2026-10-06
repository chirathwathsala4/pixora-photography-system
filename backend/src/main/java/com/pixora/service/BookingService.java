package com.pixora.service;

import com.pixora.dto.BookingRequest;
import com.pixora.dto.BookingResponse;
import com.pixora.dto.UserDto;
import com.pixora.entity.Booking;
import com.pixora.entity.Package;
import com.pixora.entity.Payment;
import com.pixora.entity.User;
import com.pixora.exception.ResourceNotFoundException;
import com.pixora.pattern.decorator.BookingPrice;
import com.pixora.pattern.decorator.DeliveryFeeDecorator;
import com.pixora.pattern.decorator.PackageBasePrice;
import com.pixora.pattern.decorator.PromoDiscountDecorator;
import com.pixora.pattern.observer.BookingEvent;
import com.pixora.pattern.observer.BookingEventPublisher;
import com.pixora.pattern.observer.BookingEventType;
import com.pixora.repository.BookingRepository;
import com.pixora.repository.PackageRepository;
import com.pixora.repository.PaymentRepository;
import com.pixora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PackageRepository packageRepository;
    private final PaymentRepository paymentRepository;
    /** Observer pattern: the subject that notifies all booking observers. */
    private final BookingEventPublisher bookingEventPublisher;

    @Transactional
    public BookingResponse createBooking(BookingRequest request, Long clientId) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        Package pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        User photographer = null;
        if (request.getPhotographerId() != null) {
            photographer = userRepository.findById(request.getPhotographerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Photographer not found"));
            List<Booking> conflicts = bookingRepository.findByPhotographerAndDate(
                    request.getPhotographerId(), request.getEventDate());
            if (!conflicts.isEmpty()) {
                throw new RuntimeException("Photographer is not available on the selected date");
            }
        }

        BigDecimal deliveryFee = request.getDeliveryFeeLkr() != null ? request.getDeliveryFeeLkr() : BigDecimal.ZERO;
        BigDecimal discount = request.getDiscountAmountLkr() != null ? request.getDiscountAmountLkr() : BigDecimal.ZERO;

        // Decorator pattern: start with the package price and wrap it with
        // a delivery fee, then a promo discount.
        BookingPrice price = new PackageBasePrice(pkg);
        price = new DeliveryFeeDecorator(price, deliveryFee);
        price = new PromoDiscountDecorator(price, discount);
        BigDecimal totalAmount = price.getAmount();

        Booking booking = Booking.builder()
                .client(client)
                .photographer(photographer)
                .pkg(pkg)
                .eventDate(request.getEventDate())
                .eventTime(request.getEventTime())
                .venueAddress(request.getVenueAddress())
                .totalAmountLkr(totalAmount)
                .addons(request.getAddons())
                .deliveryTier(request.getDeliveryTier() != null ? request.getDeliveryTier() : "STANDARD")
                .deliveryFeeLkr(deliveryFee)
                .discountAmountLkr(discount)
                .promoCode(request.getPromoCode())
                .clientNotes(request.getClientNotes())
                .status(Booking.BookingStatus.PENDING_ADMIN_APPROVAL)
                .staffStatus(photographer != null ? Booking.StaffStatus.PENDING_ACCEPTANCE : Booking.StaffStatus.UNSTAFFED)
                .build();

        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getClientBookings(Long clientId) {
        return bookingRepository.findByClientUserId(clientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getPhotographerBookings(Long photographerId) {
        return bookingRepository.findByPhotographerUserId(photographerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        return toResponse(findBooking(bookingId));
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse completeBooking(Long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.setStatus(Booking.BookingStatus.COMPLETED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public void cancelPendingBooking(Long bookingId) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != Booking.BookingStatus.PENDING_ADMIN_APPROVAL) {
            throw new RuntimeException("Only PENDING_ADMIN_APPROVAL bookings can be hard-deleted by the client.");
        }
        bookingRepository.deleteById(bookingId);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse assignPhotographer(Long bookingId, Long photographerId) {
        Booking booking = findBooking(bookingId);
        User photographer = userRepository.findById(photographerId)
                .orElseThrow(() -> new ResourceNotFoundException("Photographer not found with id: " + photographerId));

        if (photographer.getRole() != User.Role.PHOTOGRAPHER) {
            throw new RuntimeException("Selected user is not a photographer");
        }
        if (photographer.getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw new RuntimeException("Photographer account is not active");
        }

        // Conflict check: photographer already has STAFFED or PENDING_ACCEPTANCE booking on same date (excluding this booking)
        List<Booking> conflicts = bookingRepository.findByPhotographerAndDate(photographerId, booking.getEventDate());
        boolean hasConflict = conflicts.stream()
                .filter(b -> !b.getBookingId().equals(bookingId))
                .anyMatch(b -> b.getStaffStatus() == Booking.StaffStatus.STAFFED
                        || b.getStaffStatus() == Booking.StaffStatus.PENDING_ACCEPTANCE);
        if (hasConflict) {
            throw new RuntimeException("Photographer has a scheduling conflict on this date. Please choose a different photographer or date.");
        }

        booking.setPhotographer(photographer);
        booking.setStaffStatus(Booking.StaffStatus.PENDING_ACCEPTANCE);
        Booking saved = bookingRepository.save(booking);

        // Observer pattern: publish the event; observers notify the photographer and log it.
        bookingEventPublisher.notifyObservers(new BookingEvent(
                BookingEventType.ASSIGNMENT_REQUEST,
                bookingId,
                "New Assignment Request",
                "You have been assigned to Booking #" + bookingId + " on " + booking.getEventDate() + ". Please accept or decline.",
                List.of(photographer)
        ));

        return toResponse(saved);
    }

    @Transactional
    public BookingResponse unassignPhotographer(Long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.setPhotographer(null);
        booking.setStaffStatus(Booking.StaffStatus.UNSTAFFED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse respondToAssignment(Long bookingId, Long photographerId, String action) {
        Booking booking = findBooking(bookingId);

        if (booking.getPhotographer() == null || !booking.getPhotographer().getUserId().equals(photographerId)) {
            throw new RuntimeException("You are not the assigned photographer for this booking");
        }
        if (booking.getStaffStatus() != Booking.StaffStatus.PENDING_ACCEPTANCE) {
            throw new RuntimeException("This booking is not awaiting your acceptance");
        }

        if ("ACCEPT".equalsIgnoreCase(action)) {
            booking.setStaffStatus(Booking.StaffStatus.STAFFED);
            bookingRepository.save(booking);

            // Observer pattern: tell all admins the photographer accepted.
            bookingEventPublisher.notifyObservers(new BookingEvent(
                    BookingEventType.ASSIGNMENT_ACCEPTED,
                    bookingId,
                    "Photographer Accepted Assignment",
                    booking.getPhotographer().getFullName() + " accepted Booking #" + bookingId + " on " + booking.getEventDate(),
                    findAdmins()
            ));

        } else if ("DECLINE".equalsIgnoreCase(action)) {
            User previousPhotographer = booking.getPhotographer();
            booking.setStaffStatus(Booking.StaffStatus.UNSTAFFED);
            booking.setPhotographer(null);
            bookingRepository.save(booking);

            // Observer pattern: tell all admins a new photographer is needed.
            bookingEventPublisher.notifyObservers(new BookingEvent(
                    BookingEventType.ASSIGNMENT_DECLINED,
                    bookingId,
                    "Photographer Declined — Reassignment Needed",
                    previousPhotographer.getFullName() + " declined Booking #" + bookingId + " on " + booking.getEventDate() + ". Please assign a new photographer.",
                    findAdmins()
            ));

        } else {
            throw new RuntimeException("Invalid action. Must be ACCEPT or DECLINE");
        }

        return toResponse(findBooking(bookingId));
    }

    @Transactional
    public void deleteBooking(Long bookingId) {
        Booking booking = findBooking(bookingId);
        bookingRepository.deleteById(booking.getBookingId());
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAvailablePhotographers(LocalDate date) {
        List<Long> bookedIds = bookingRepository.findBookedPhotographerIdsByDate(date);
        List<User> allActive = userRepository.findByRoleAndAccountStatus(
                User.Role.PHOTOGRAPHER, User.AccountStatus.ACTIVE);
        return allActive.stream()
                .filter(p -> !bookedIds.contains(p.getUserId()))
                .map(p -> UserDto.builder()
                        .userId(p.getUserId())
                        .fullName(p.getFullName())
                        .email(p.getEmail())
                        .phone(p.getPhone())
                        .portfolioUrl(p.getPortfolioUrl())
                        .accountStatus(p.getAccountStatus().name())
                        .role(p.getRole().name())
                        .build())
                .collect(Collectors.toList());
    }

    /** @return every user with the ADMIN role. */
    private List<User> findAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.ADMIN)
                .collect(Collectors.toList());
    }

    public Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    @Transactional
    public BookingResponse updateClientBooking(Long bookingId, Long clientId, com.pixora.dto.BookingUpdateRequest request) {
        Booking booking = findBooking(bookingId);
        if (!booking.getClient().getUserId().equals(clientId)) {
            throw new RuntimeException("Unauthorized to modify this booking");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED || booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot edit a " + booking.getStatus() + " booking.");
        }

        boolean scheduleOrVenueChanged = !booking.getEventDate().equals(request.getEventDate()) ||
                !booking.getEventTime().equals(request.getEventTime()) ||
                !booking.getVenueAddress().equals(request.getVenueAddress());

        booking.setEventDate(request.getEventDate());
        booking.setEventTime(request.getEventTime());
        booking.setVenueAddress(request.getVenueAddress());
        if (request.getClientNotes() != null) {
            booking.setClientNotes(request.getClientNotes());
        }

        // If photographer was assigned, reset to PENDING_ACCEPTANCE and notify them
        if (scheduleOrVenueChanged && booking.getPhotographer() != null) {
            booking.setStaffStatus(Booking.StaffStatus.PENDING_ACCEPTANCE);
            // Observer pattern: tell the photographer the schedule changed.
            bookingEventPublisher.notifyObservers(new BookingEvent(
                    BookingEventType.BOOKING_UPDATE,
                    bookingId,
                    "Booking Schedule Updated — Re-Approval Needed",
                    "Client updated Booking #" + bookingId + " date/time to " + request.getEventDate() + " at " + request.getEventTime() + ". Please re-accept.",
                    List.of(booking.getPhotographer())
            ));
        }

        return toResponse(bookingRepository.save(booking));
    }

    public BookingResponse toResponse(Booking b) {
        Optional<Payment> payment = paymentRepository.findByBookingBookingId(b.getBookingId());
        return BookingResponse.builder()
                .bookingId(b.getBookingId())
                .clientId(b.getClient().getUserId())
                .clientName(b.getClient().getFullName())
                .photographerId(b.getPhotographer() != null ? b.getPhotographer().getUserId() : null)
                .photographerName(b.getPhotographer() != null ? b.getPhotographer().getFullName() : null)
                .packageId(b.getPkg().getPackageId())
                .packageName(b.getPkg().getPackageName())
                .priceLkr(b.getPkg().getPriceLkr())
                .eventDate(b.getEventDate())
                .eventTime(b.getEventTime())
                .venueAddress(b.getVenueAddress())
                .totalAmountLkr(b.getTotalAmountLkr())
                .addons(b.getAddons())
                .deliveryTier(b.getDeliveryTier())
                .deliveryFeeLkr(b.getDeliveryFeeLkr())
                .discountAmountLkr(b.getDiscountAmountLkr())
                .promoCode(b.getPromoCode())
                .clientNotes(b.getClientNotes())
                .status(b.getStatus().name())
                .staffStatus(b.getStaffStatus() != null ? b.getStaffStatus().name() : "UNSTAFFED")
                .paymentStatus(payment.map(p -> p.getPaymentStatus().name()).orElse(null))
                .build();
    }
}