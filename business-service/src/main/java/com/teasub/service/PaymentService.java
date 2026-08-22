package com.teasub.service;

import com.teasub.exception.BusinessException;
import com.teasub.model.AuditLog;
import com.teasub.model.Payment;
import com.teasub.model.Subscription;
import com.teasub.repository.AuditLogRepository;
import com.teasub.repository.PaymentRepository;
import com.teasub.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Reconciles Razorpay events. Idempotency is enforced by the unique index on
 * {@code razorpayPaymentId} (see Payment entity) — replays of the same webhook event, which
 * Razorpay explicitly documents as possible, are safe no-ops rather than double-activations.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final AuditLogRepository auditLogRepository;

    public PaymentService(PaymentRepository paymentRepository,
                           SubscriptionRepository subscriptionRepository,
                           SubscriptionService subscriptionService,
                           AuditLogRepository auditLogRepository) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(transactionManager = "transactionManager")
    public Map<String, String> confirm(String razorpayOrderId, String razorpayPaymentId, String customerId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> BusinessException.notFound("Order"));

        // Idempotent: if this payment id was already reconciled (e.g. by the webhook racing
        // ahead of this client-confirmation call), do nothing further and report success.
        Optional<Payment> already = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId);
        if (already.isPresent() && "PAID".equals(already.get().getStatus())) {
            return Map.of("status", "PAID", "message", "Payment already confirmed");
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus("PAID");
        paymentRepository.save(payment); // unique index guards a genuine double-submit race

        Subscription sub = subscriptionRepository.findById(
                subscriptionRepository.findByCustomerIdOrderByStartDateDesc(customerId).get(0).getId()
        ).orElseThrow(() -> BusinessException.notFound("Subscription"));
        subscriptionService.activate(sub);

        auditLogRepository.save(AuditLog.of(customerId, "customer", "PAYMENT_CONFIRMED", payment.getId(),
                Map.of("amount", payment.getAmount(), "razorpayPaymentId", razorpayPaymentId)));

        return Map.of("status", "PAID", "message", "Subscription activated");
    }

    /** Handles the raw Razorpay webhook event forwarded (already signature-verified) by the gateway. */
    @Transactional(transactionManager = "transactionManager")
    public void handleWebhookEvent(Map<String, Object> event) {
        String eventType = String.valueOf(event.get("event"));
        if (!"payment.captured".equals(eventType) && !"payment.failed".equals(eventType)) return;

        @SuppressWarnings("unchecked")
        Map<String, Object> payloadPayment = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) event.get("payload")).get("payment")).get("entity");
        String razorpayPaymentId = String.valueOf(payloadPayment.get("id"));
        String razorpayOrderId = String.valueOf(payloadPayment.get("order_id"));

        if (paymentRepository.findByRazorpayPaymentId(razorpayPaymentId).isPresent()) {
            return; // already reconciled — idempotent no-op
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (payment == null) return; // unknown order, ignore defensively

        if ("payment.captured".equals(eventType)) {
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setStatus("PAID");
            paymentRepository.save(payment);
        } else {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            auditLogRepository.save(AuditLog.of("system", "system", "PAYMENT_FAILED", payment.getId(), Map.of("razorpayOrderId", razorpayOrderId)));
        }
    }
}
