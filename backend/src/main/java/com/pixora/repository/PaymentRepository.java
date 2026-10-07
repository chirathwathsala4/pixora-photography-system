package com.pixora.repository;

import com.pixora.entity.Payment;
//provides many database operations automatically,save()findById()findAll()delete()deleteById()
import org.springframework.data.jpa.repository.JpaRepository;
//Spring that this interface is a repository component
import org.springframework.stereotype.Repository;

import java.util.Optional;

//Spring that this component is responsible for database access.
@Repository
//provides CRUD operations for the Payment entity
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingBookingId(Long bookingId);
}
