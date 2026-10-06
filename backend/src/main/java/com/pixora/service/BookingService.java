package com.pixora.service; // Service layer

import com.pixora.dto.BookingRequest;
import com.pixora.dto.BookingResponse;
import com.pixora.dto.UserDto;
import com.pixora.entity.Booking;
import com.pixora.entity.Package;
import com.pixora.entity.Payment;
import com.pixora.entity.User;
import com.pixora.exception.ResourceNotFoundException;
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

@Service // Marks this as a service/business logic class
@RequiredArgsConstructor // Creates constructor for final fields
public class BookingService {

    // Repositories used to access database
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PackageRepository packageRepository;
    private final PaymentRepository paymentRepository;

    // Used to send booking-related notifications
    private final NotificationService notificationService;


    @Transactional
    public BookingResponse createBooking(BookingRequest request, Long clientId) {

        // Find the client
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Find selected package
        Package pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        User photographer = null;

        // If photographer was selected
        if (request.getPhotographerId() != null) {

            // Find photographer
            photographer = userRepository.findById(request.getPhotographerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Photographer not found"));

            // Check photographer availability
            List<Booking> conflicts = bookingRepository.findByPhotographerAndDate(
                    request.getPhotographerId(), request.getEventDate());

            if (!conflicts.isEmpty()) {
                throw new RuntimeException(
                        "Photographer is not available on the selected date");
            }
        }

        // Calculate booking price
        BigDecimal basePrice = pkg.getPriceLkr();

        BigDecimal deliveryFee =
                request.getDeliveryFeeLkr() != null
                        ? request.getDeliveryFeeLkr()
                        : BigDecimal.ZERO;

        BigDecimal discount =
                request.getDiscountAmountLkr() != null
                        ? request.getDiscountAmountLkr()
                        : BigDecimal.ZERO;

        // Total = package price + delivery fee - discount
        BigDecimal totalAmount =
                basePrice.add(deliveryFee)
                         .subtract(discount)
                         .max(BigDecimal.ZERO);

        // Create Booking entity using Builder
        Booking booking = Booking.builder()
                .client(client)
                .photographer(photographer)
                .pkg(pkg)
                .eventDate(request.getEventDate())
                .eventTime(request.getEventTime())
                .venueAddress(request.getVenueAddress())
                .totalAmountLkr(totalAmount)
                .addons(request.getAddons())
                .deliveryTier(
                        request.getDeliveryTier() != null
                                ? request.getDeliveryTier()
                                : "STANDARD")
                .deliveryFeeLkr(deliveryFee)
                .discountAmountLkr(discount)
                .promoCode(request.getPromoCode())
                .clientNotes(request.getClientNotes())

                // Initial booking status
                .status(Booking.BookingStatus.PENDING_ADMIN_APPROVAL)

                // Photographer assignment status
                .staffStatus(
                        photographer != null
                                ? Booking.StaffStatus.PENDING_ACCEPTANCE
                                : Booking.StaffStatus.UNSTAFFED)
                .build();

        // Save booking and convert to response
        return toResponse(bookingRepository.save(booking));
    }


    @Transactional(readOnly = true)
    public List<BookingResponse> getClientBookings(Long clientId) {

        // Get all bookings belonging to a client
        return bookingRepository.findByClientUserId(clientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<BookingResponse> getPhotographerBookings(Long photographerId) {

        // Get all bookings assigned to photographer
        return bookingRepository.findByPhotographerUserId(photographerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {

        // Get all bookings
        return bookingRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {

        // Find one booking by ID
        return toResponse(findBooking(bookingId));
    }


    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {

        // Change booking status to CONFIRMED
        Booking booking = findBooking(bookingId);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        return toResponse(bookingRepository.save(booking));
    }


    @Transactional
    public BookingResponse completeBooking(Long bookingId) {

        // Mark booking as completed
        Booking booking = findBooking(bookingId);
        booking.setStatus(Booking.BookingStatus.COMPLETED);

        return toResponse(bookingRepository.save(booking));
    }


    @Transactional
    public void cancelPendingBooking(Long bookingId) {

        // Only pending bookings can be hard-deleted
        Booking booking = findBooking(bookingId);

        if (booking.getStatus() !=
                Booking.BookingStatus.PENDING_ADMIN_APPROVAL) {

            throw new RuntimeException(
                    "Only PENDING_ADMIN_APPROVAL bookings can be hard-deleted by the client.");
        }

        bookingRepository.deleteById(bookingId);
    }


    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {

        // Cancel booking without deleting it
        Booking booking = findBooking(bookingId);
        booking.setStatus(Booking.BookingStatus.CANCELLED);

        return toResponse(bookingRepository.save(booking));
    }


    @Transactional
    public BookingResponse assignPhotographer(
            Long bookingId, Long photographerId) {

        // Find booking
        Booking booking = findBooking(bookingId);

        // Find photographer
        User photographer = userRepository.findById(photographerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Photographer not found with id: " + photographerId));

        // Check user is actually a photographer
        if (photographer.getRole() != User.Role.PHOTOGRAPHER) {
            throw new RuntimeException(
                    "Selected user is not a photographer");
        }

        // Check photographer account is active
        if (photographer.getAccountStatus() !=
                User.AccountStatus.ACTIVE) {

            throw new RuntimeException(
                    "Photographer account is not active");
        }

        // Check photographer scheduling conflicts
        List<Booking> conflicts =
                bookingRepository.findByPhotographerAndDate(
                        photographerId,
                        booking.getEventDate());

        boolean hasConflict = conflicts.stream()

                // Exclude current booking
                .filter(b -> !b.getBookingId().equals(bookingId))

                // Check active assignments
                .anyMatch(b ->
                        b.getStaffStatus() ==
                                Booking.StaffStatus.STAFFED
                        ||
                        b.getStaffStatus() ==
                                Booking.StaffStatus.PENDING_ACCEPTANCE);

        if (hasConflict) {
            throw new RuntimeException(
                    "Photographer has a scheduling conflict on this date.");
        }

        // Assign photographer
        booking.setPhotographer(photographer);

        // Waiting for photographer's response
        booking.setStaffStatus(
                Booking.StaffStatus.PENDING_ACCEPTANCE);

        Booking saved = bookingRepository.save(booking);

        // Notify photographer
        notificationService.sendNotification(
                photographer,
                "New Assignment Request",
                "You have been assigned to Booking #" + bookingId,
                "ASSIGNMENT_REQUEST",
                bookingId
        );

        return toResponse(saved);
    }


    @Transactional
    public BookingResponse unassignPhotographer(Long bookingId) {

        // Remove photographer from booking
        Booking booking = findBooking(bookingId);

        booking.setPhotographer(null);
        booking.setStaffStatus(Booking.StaffStatus.UNSTAFFED);

        return toResponse(bookingRepository.save(booking));
    }


    @Transactional
    public BookingResponse respondToAssignment(
            Long bookingId,
            Long photographerId,
            String action) {

        Booking booking = findBooking(bookingId);

        // Verify photographer is assigned
        if (booking.getPhotographer() == null ||
                !booking.getPhotographer()
                        .getUserId()
                        .equals(photographerId)) {

            throw new RuntimeException(
                    "You are not the assigned photographer");
        }

        // Check assignment is waiting for response
        if (booking.getStaffStatus() !=
                Booking.StaffStatus.PENDING_ACCEPTANCE) {

            throw new RuntimeException(
                    "This booking is not awaiting your acceptance");
        }


        // Photographer accepts assignment
        if ("ACCEPT".equalsIgnoreCase(action)) {

            booking.setStaffStatus(
                    Booking.StaffStatus.STAFFED);

            bookingRepository.save(booking);

            // Notify administrators
            userRepository.findAll().stream()
                    .filter(u ->
                            u.getRole() == User.Role.ADMIN)
                    .forEach(admin ->
                            notificationService.sendNotification(
                                    admin,
                                    "Photographer Accepted Assignment",
                                    booking.getPhotographer()
                                            .getFullName()
                                            + " accepted Booking #"
                                            + bookingId,
                                    "ASSIGNMENT_ACCEPTED",
                                    bookingId
                            ));

        }

        // Photographer declines assignment
        else if ("DECLINE".equalsIgnoreCase(action)) {

            User previousPhotographer =
                    booking.getPhotographer();

            booking.setStaffStatus(
                    Booking.StaffStatus.UNSTAFFED);

            booking.setPhotographer(null);

            bookingRepository.save(booking);

            // Notify admin to assign another photographer
            userRepository.findAll().stream()
                    .filter(u ->
                            u.getRole() == User.Role.ADMIN)
                    .forEach(admin ->
                            notificationService.sendNotification(
                                    admin,
                                    "Photographer Declined",
                                    previousPhotographer
                                            .getFullName()
                                            + " declined Booking #"
                                            + bookingId,
                                    "ASSIGNMENT_DECLINED",
                                    bookingId
                            ));
        }

        else {
            throw new RuntimeException(
                    "Invalid action. Must be ACCEPT or DECLINE");
        }

        return toResponse(findBooking(bookingId));
    }


    @Transactional
    public void deleteBooking(Long bookingId) {

        // Delete booking permanently
        Booking booking = findBooking(bookingId);

        bookingRepository.deleteById(
                booking.getBookingId());
    }


    @Transactional(readOnly = true)
    public List<UserDto> getAvailablePhotographers(
            LocalDate date) {

        // Get IDs of already booked photographers
        List<Long> bookedIds =
                bookingRepository
                        .findBookedPhotographerIdsByDate(date);

        // Get all active photographers
        List<User> allActive =
                userRepository.findByRoleAndAccountStatus(
                        User.Role.PHOTOGRAPHER,
                        User.AccountStatus.ACTIVE);

        // Remove photographers who are already booked
        return allActive.stream()
                .filter(p ->
                        !bookedIds.contains(p.getUserId()))

                // Convert User entity to UserDto
                .map(p -> UserDto.builder()
                        .userId(p.getUserId())
                        .fullName(p.getFullName())
                        .email(p.getEmail())
                        .phone(p.getPhone())
                        .portfolioUrl(p.getPortfolioUrl())
                        .accountStatus(
                                p.getAccountStatus().name())
                        .role(p.getRole().name())
                        .build())
                .collect(Collectors.toList());
    }


    public Booking findBooking(Long id) {

        // Find booking or throw error if not found
        return bookingRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with id: " + id));
    }


    @Transactional
    public BookingResponse updateClientBooking(
            Long bookingId,
            Long clientId,
            com.pixora.dto.BookingUpdateRequest request) {

        // Find booking
        Booking booking = findBooking(bookingId);

        // Check booking belongs to this client
        if (!booking.getClient()
                .getUserId()
                .equals(clientId)) {

            throw new RuntimeException(
                    "Unauthorized to modify this booking");
        }

        // Completed/cancelled bookings cannot be edited
        if (booking.getStatus() ==
                Booking.BookingStatus.CANCELLED
                ||
                booking.getStatus() ==
                        Booking.BookingStatus.COMPLETED) {

            throw new RuntimeException(
                    "Cannot edit a " +
                    booking.getStatus() +
                    " booking.");
        }

        // Check whether date, time or venue changed
        boolean scheduleOrVenueChanged =
                !booking.getEventDate()
                        .equals(request.getEventDate())
                ||
                !booking.getEventTime()
                        .equals(request.getEventTime())
                ||
                !booking.getVenueAddress()
                        .equals(request.getVenueAddress());

        // Update booking details
        booking.setEventDate(request.getEventDate());
        booking.setEventTime(request.getEventTime());
        booking.setVenueAddress(request.getVenueAddress());

        if (request.getClientNotes() != null) {
            booking.setClientNotes(
                    request.getClientNotes());
        }

        // If photographer is assigned and schedule changed,
        // photographer must approve again
        if (scheduleOrVenueChanged &&
                booking.getPhotographer() != null) {

            booking.setStaffStatus(
                    Booking.StaffStatus.PENDING_ACCEPTANCE);

            notificationService.sendNotification(
                    booking.getPhotographer(),
                    "Booking Schedule Updated",
                    "Client updated Booking #" +
                            bookingId +
                            ". Please re-accept.",
                    "BOOKING_UPDATE",
                    bookingId
            );
        }

        return toResponse(
                bookingRepository.save(booking));
    }


    // Convert Booking entity into BookingResponse DTO
    public BookingResponse toResponse(Booking b) {

        // Find payment related to booking
        Optional<Payment> payment =
                paymentRepository
                        .findByBookingBookingId(
                                b.getBookingId());

        // Build response object
        return BookingResponse.builder()
                .bookingId(b.getBookingId())

                .clientId(
                        b.getClient().getUserId())
                .clientName(
                        b.getClient().getFullName())

                .photographerId(
                        b.getPhotographer() != null
                                ? b.getPhotographer().getUserId()
                                : null)

                .photographerName(
                        b.getPhotographer() != null
                                ? b.getPhotographer().getFullName()
                                : null)

                .packageId(
                        b.getPkg().getPackageId())
                .packageName(
                        b.getPkg().getPackageName())
                .priceLkr(
                        b.getPkg().getPriceLkr())

                .eventDate(b.getEventDate())
                .eventTime(b.getEventTime())
                .venueAddress(b.getVenueAddress())

                .totalAmountLkr(
                        b.getTotalAmountLkr())

                .addons(b.getAddons())
                .deliveryTier(b.getDeliveryTier())
                .deliveryFeeLkr(b.getDeliveryFeeLkr())
                .discountAmountLkr(
                        b.getDiscountAmountLkr())
                .promoCode(b.getPromoCode())
                .clientNotes(b.getClientNotes())

                .status(b.getStatus().name())

                .staffStatus(
                        b.getStaffStatus() != null
                                ? b.getStaffStatus().name()
                                : "UNSTAFFED")

                .paymentStatus(
                        payment.map(p ->
                                p.getPaymentStatus().name())
                                .orElse(null))

                .build();
    }
}