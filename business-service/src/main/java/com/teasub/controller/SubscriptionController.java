package com.teasub.controller;

import com.teasub.model.Subscription;
import com.teasub.service.SubscriptionService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    public record CheckoutRequest(@NotBlank String customerId, String plan) {}

    @PostMapping("/checkout")
    public SubscriptionService.CheckoutResult checkout(@RequestBody CheckoutRequest req) {
        return subscriptionService.startCheckout(req.customerId());
    }

    @GetMapping("/customer/{customerId}")
    public Subscription current(@PathVariable String customerId) {
        return subscriptionService.currentFor(customerId);
    }

    @GetMapping("/customer/{customerId}/history")
    public List<Subscription> history(@PathVariable String customerId) {
        return subscriptionService.historyFor(customerId);
    }
}
