package com.teasub.repository;

import com.teasub.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    long countByStatus(String status);
}
