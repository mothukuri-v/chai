package com.teasub.controller;

import com.teasub.service.PaymentService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public record ConfirmRequest(@NotBlank String razorpayOrderId, @NotBlank String razorpayPaymentId, @NotBlank String customerId) {}

    @PostMapping("/confirm")
    public Map<String, String> confirm(@RequestBody ConfirmRequest req) {
        return paymentService.confirm(req.razorpayOrderId(), req.razorpayPaymentId(), req.customerId());
    }

    @PostMapping("/webhook-event")
    public Map<String, Boolean> webhookEvent(@RequestBody Map<String, Object> event) {
        paymentService.handleWebhookEvent(event);
        return Map.of("processed", true);
    }
}
