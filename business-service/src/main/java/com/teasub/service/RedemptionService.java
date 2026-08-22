package com.teasub.service;

import com.teasub.exception.BusinessException;
import com.teasub.model.AuditLog;
import com.teasub.model.Redemption;
import com.teasub.model.Shop;
import com.teasub.model.Subscription;
import com.teasub.repository.AuditLogRepository;
import com.teasub.repository.RedemptionRepository;
import com.teasub.repository.ShopRepository;
import com.teasub.repository.SubscriptionRepository;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

/**
 * The critical redemption path described in the architecture doc:
 *   validate QR -> validate subscription -> check today's redemption -> create
 *   redemption transaction -> return SUCCESS.
 *
 * The actual "one tea per day" guarantee comes from the unique compound index on
 * {@link Redemption} (customerId, redemptionDate) — the pre-checks here exist purely to
 * fail fast with a friendly, specific error before hitting the database, not as the
 * source of truth. {@link MongoTransactionManager} requires a MongoDB replica set (even a
 * single-node one in dev — see infra/docker-compose.yml).
 */
@Service
public class RedemptionService {

    private final QrService qrService;
    private final SubscriptionRepository subscriptionRepository;
    private final RedemptionRepository redemptionRepository;
    private final ShopRepository shopRepository;
    private final AuditLogRepository auditLogRepository;
    private final ZoneId zone = ZoneId.of("Asia/Kolkata");

    public RedemptionService(QrService qrService,
                              SubscriptionRepository subscriptionRepository,
                              RedemptionRepository redemptionRepository,
                              ShopRepository shopRepository,
                              AuditLogRepository auditLogRepository) {
        this.qrService = qrService;
        this.subscriptionRepository = subscriptionRepository;
        this.redemptionRepository = redemptionRepository;
        this.shopRepository = shopRepository;
        this.auditLogRepository = auditLogRepository;
    }

    private String today() {
        return LocalDate.now(zone).toString();
    }

    public record TodayStatus(boolean redeemedToday, boolean subscriptionActive, int teaCreditsRemaining, String subscriptionEndsOn) {}

    public TodayStatus today(String customerId) {
        Subscription sub = activeSubscriptionOrNull(customerId);
        boolean redeemed = redemptionRepository.existsByCustomerIdAndRedemptionDate(customerId, today());
        return new TodayStatus(
                redeemed,
                sub != null,
                sub != null ? sub.getTeaCreditsRemaining() : 0,
                sub != null ? sub.getEndDate().toString() : null
        );
    }

    /** Step 1 of the flow: mint a QR token, but only after re-validating eligibility. */
    public QrService.IssuedToken issueToken(String customerId) {
        Subscription sub = activeSubscriptionOrNull(customerId);
        if (sub == null) throw BusinessException.subscriptionInactive();

        if (redemptionRepository.existsByCustomerIdAndRedemptionDate(customerId, today())) {
            throw BusinessException.alreadyRedeemedToday();
        }

        return qrService.issue(customerId);
    }

    public record RedemptionResult(String status, String customerId, String shopName, String redeemedAt, int teaCreditsRemaining) {}

    /**
     * Step 2 of the flow, run by the shop owner's scan. This is the single method that
     * enforces "prevent duplicate redemptions, expired subscriptions, fake QR codes,
     * unauthorized shops" all together, atomically.
     */
    @Transactional(transactionManager = "transactionManager")
    public RedemptionResult validate(String qrToken, String shopOwnerId) {
        // 1. Validate QR signature + expiry (fake/tampered/expired QR rejected here)
        QrService.DecodedToken decoded = qrService.verify(qrToken);

        // 2. Resolve & authorize the scanning shop (unauthorized shop rejected here)
        Shop shop = shopRepository.findByOwnerId(shopOwnerId).stream().findFirst()
                .orElseThrow(() -> BusinessException.notFound("Shop"));
        if (!shop.isOperational()) throw BusinessException.shopNotVerified();

        // 3. Re-validate subscription independently of what the QR "claims" (expired sub rejected here)
        Subscription sub = activeSubscriptionOrNull(decoded.customerId());
        if (sub == null) throw BusinessException.subscriptionInactive();

        // 4. Re-check today's redemption (fast-fail before the unique-index write)
        String today = today();
        if (redemptionRepository.existsByCustomerIdAndRedemptionDate(decoded.customerId(), today)) {
            throw BusinessException.alreadyRedeemedToday();
        }

        // 5. Create the redemption — the unique index (customerId, redemptionDate) is the
        //    real, race-proof guarantee; a concurrent duplicate throws DuplicateKeyException,
        //    mapped to ALREADY_REDEEMED_TODAY by GlobalExceptionHandler.
        Redemption redemption = new Redemption();
        redemption.setCustomerId(decoded.customerId());
        redemption.setShopId(shop.getId());
        redemption.setSubscriptionId(sub.getId());
        redemption.setRedemptionDate(today);
        redemption.setJti(decoded.jti()); // unique index also blocks QR replay
        redemptionRepository.save(redemption);

        // 6. Decrement the subscription's tea credit atomically within the same transaction
        sub.setTeaCreditsRemaining(sub.getTeaCreditsRemaining() - 1);
        subscriptionRepository.save(sub);

        // 7. Audit trail
        auditLogRepository.save(AuditLog.of(shopOwnerId, "shop_owner", "REDEMPTION_SUCCESS", redemption.getId(),
                Map.of("customerId", decoded.customerId(), "shopId", shop.getId())));

        return new RedemptionResult("SUCCESS", decoded.customerId(), shop.getName(),
                redemption.getRedeemedAt().toString(), sub.getTeaCreditsRemaining());
    }

    private Subscription activeSubscriptionOrNull(String customerId) {
        Optional<Subscription> sub = subscriptionRepository.findFirstByCustomerIdAndStatusOrderByEndDateDesc(customerId, "ACTIVE");
        return sub.filter(Subscription::isCurrentlyActive).orElse(null);
    }
}
