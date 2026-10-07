package com.pixora.service;

import com.pixora.dto.PaymentRequest;
import com.pixora.dto.PaymentResponse;
import com.pixora.entity.Booking;
import com.pixora.entity.Payment;
import com.pixora.exception.ResourceNotFoundException;
import com.pixora.pattern.factory.PaymentStrategyFactory;
import com.pixora.pattern.strategy.PaymentContext;
import com.pixora.pattern.strategy.PaymentStrategy;
import com.pixora.repository.BookingRepository;
import com.pixora.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public PaymentResponse submitPayment(Long bookingId, PaymentRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Check if payment already exists and is approved or paid
        Optional<Payment> existing = paymentRepository.findByBookingBookingId(bookingId);
        if (existing.isPresent()) {
            Payment.PaymentStatus currentStatus = existing.get().getPaymentStatus();
            if (currentStatus == Payment.PaymentStatus.APPROVED || currentStatus == Payment.PaymentStatus.PAID) {
                throw new RuntimeException("Payment already completed for this booking");
            }
        }

        // Factory pattern: the factory decides which payment strategy to create.Card,Online Transer
        PaymentStrategy strategy = PaymentStrategyFactory.createStrategy(request);

        // Strategy pattern: the context runs whichever strategy(Card,Online Transer) it was given.
        PaymentContext paymentContext = new PaymentContext(strategy);

        Payment payment = existing.orElse(Payment.builder().booking(booking).build());
        paymentContext.executePayment(payment, booking, request);

        Payment savedPayment = paymentRepository.save(payment);
        bookingRepository.save(booking);
        return toResponse(savedPayment);
    }

    @Transactional
    public PaymentResponse approvePayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        //Payment is approved by the admin
        payment.setPaymentStatus(Payment.PaymentStatus.APPROVED);
        paymentRepository.save(payment);

        // Auto-confirm the booking
        Booking booking = payment.getBooking();
        //Booking is confirm automatically
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse rejectPayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        //Admin reject the payment & reject the status saved in database
        payment.setPaymentStatus(Payment.PaymentStatus.REJECTED);
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public void deletePayment(Long paymentId) {
        //Payment & remoove it from the database
        Payment payment = findPayment(paymentId);
        paymentRepository.delete(payment);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentResponse> getPaymentByBooking(Long bookingId) {
        //Find the payment from booking ID
        return paymentRepository.findByBookingBookingId(bookingId).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        //Retrieves all payments from the database.
        return paymentRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }


    //It searches for a payment using its ID. If the payment doesn't exist, it throws an error.
    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    public PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .bookingId(p.getBooking().getBookingId())
                .transactionRef(p.getTransactionRef())
                .amountPaidLkr(p.getAmountPaidLkr())
                .paymentStatus(p.getPaymentStatus().name())
                .clientName(p.getBooking().getClient().getFullName())
                .packageName(p.getBooking().getPkg().getPackageName())
                .build();
    }
}
