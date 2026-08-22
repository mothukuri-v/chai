package com.teasub.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "subscriptions")
public class Subscription {

    @Id
    private String id;

    @Indexed
    private String customerId;

    private String plan = "MONTHLY_30";
    private int amount;
    private Instant startDate;
    private Instant endDate;
    private int teaCreditsTotal;
    private int teaCreditsRemaining;

    /** PENDING (awaiting payment) -> ACTIVE -> EXPIRED | CANCELLED */
    private String status;

    private String paymentId;

    public boolean isCurrentlyActive() {
        return "ACTIVE".equals(status) && endDate != null && endDate.isAfter(Instant.now()) && teaCreditsRemaining > 0;
    }
}
