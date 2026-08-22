package com.teasub.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * The compound unique index below is the actual enforcement mechanism for
 * "one tea per day" — not an application-level check, which can race under
 * concurrent requests. A duplicate insert throws a MongoDB E11000 error that
 * {@link com.teasub.service.RedemptionService} maps to ALREADY_REDEEMED_TODAY.
 */
@Data
@Document(collection = "redemptions")
@CompoundIndexes({
    @CompoundIndex(name = "one_per_customer_per_day", def = "{'customerId': 1, 'redemptionDate': 1}", unique = true)
})
public class Redemption {

    @Id
    private String id;

    private String customerId;
    private String shopId;
    private String subscriptionId;

    /** Customer's local calendar day, e.g. "2026-08-22" — the unique-index partition key. */
    private String redemptionDate;

    /** QR token id (jti) — also unique, so a QR cannot be replayed even within the same day. */
    @Indexed(unique = true)
    private String jti;

    private Instant redeemedAt = Instant.now();
}
