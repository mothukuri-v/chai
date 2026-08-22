package com.teasub.service;

import com.teasub.exception.BusinessException;
import com.teasub.model.Payment;
import com.teasub.model.Subscription;
import com.teasub.repository.PaymentRepository;
import com.teasub.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    @Value("${chaipass.subscription.plan-amount-inr}")
    private int planAmount;

    @Value("${chaipass.subscription.plan-days}")
    private int planDays;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, PaymentRepository paymentRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
    }

    public record CheckoutResult(String subscriptionId, String razorpayOrderId, int amount, String currency) {}

    /**
     * Creates a PENDING subscription + payment pair before the customer ever reaches
     * Razorpay checkout, so the gateway's order-creation call and this service agree on
     * amount/currency and there's a durable record even if the customer abandons checkout.
     */
    public CheckoutResult startCheckout(String customerId) {
        Subscription sub = new Subscription();
        sub.setCustomerId(customerId);
        sub.setPlan("MONTHLY_30");
        sub.setAmount(planAmount);
        sub.setStatus("PENDING");
        sub.setTeaCreditsTotal(planDays);
        sub.setTeaCreditsRemaining(planDays);
        subscriptionRepository.save(sub);

        Payment payment = new Payment();
        payment.setCustomerId(customerId);
        payment.setAmount(planAmount);
        payment.setStatus("CREATED");
        // NOTE: real Razorpay order creation (razorpay.orders.create) happens in the gateway,
        // which then calls PaymentService.attachOrder() with the resulting order id.
        payment.setRazorpayOrderId("order_pending_" + UUID.randomUUID());
        paymentRepository.save(payment);

        sub.setPaymentId(payment.getId());
        subscriptionRepository.save(sub);

        return new CheckoutResult(sub.getId(), payment.getRazorpayOrderId(), planAmount, "INR");
    }

    /** Called after payment confirms — activates the subscription for 30 days from now. */
    public Subscription activate(Subscription sub) {
        Instant now = Instant.now();
        sub.setStartDate(now);
        sub.setEndDate(now.plus(planDays, ChronoUnit.DAYS));
        sub.setStatus("ACTIVE");
        return subscriptionRepository.save(sub);
    }

    public Subscription currentFor(String customerId) {
        return subscriptionRepository.findFirstByCustomerIdAndStatusOrderByEndDateDesc(customerId, "ACTIVE")
                .orElseThrow(() -> BusinessException.notFound("Active subscription"));
    }

    public List<Subscription> historyFor(String customerId) {
        return subscriptionRepository.findByCustomerIdOrderByStartDateDesc(customerId);
    }
}
