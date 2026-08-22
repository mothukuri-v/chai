package com.teasub.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    private String customerId;
    private String razorpayOrderId;

    /** Unique index — this is the idempotency key that makes duplicate webhook delivery a no-op. */
    @Indexed(unique = true, sparse = true)
    private String razorpayPaymentId;

    private int amount;
    private String currency = "INR";

    /** CREATED -> PAID | FAILED, or REFUNDED by admin */
    private String status = "CREATED";

    private Instant createdAt = Instant.now();
}
