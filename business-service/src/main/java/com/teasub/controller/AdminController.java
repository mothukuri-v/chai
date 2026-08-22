package com.teasub.controller;

import com.teasub.model.AuditLog;
import com.teasub.model.Shop;
import com.teasub.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final SubscriptionRepository subscriptionRepository;
    private final ShopRepository shopRepository;
    private final RedemptionRepository redemptionRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminController(SubscriptionRepository subscriptionRepository, ShopRepository shopRepository,
                            RedemptionRepository redemptionRepository, PaymentRepository paymentRepository,
                            AuditLogRepository auditLogRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.shopRepository = shopRepository;
        this.redemptionRepository = redemptionRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        String today = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();
        long activeSubs = subscriptionRepository.countByStatus("ACTIVE");
        long shops = shopRepository.findByStatus("VERIFIED").size();
        long todaysRedemptions = redemptionRepository.countByRedemptionDate(today);
        long failedPayments = paymentRepository.countByStatus("FAILED");
        long revenue = activeSubs * 500L; // simplified — real build sums PAID payments in range
        return Map.of(
                "activeSubscriptions", activeSubs,
                "verifiedShops", shops,
                "todaysRedemptions", todaysRedemptions,
                "failedPayments", failedPayments,
                "revenueInr", revenue
        );
    }

    @GetMapping("/shops/pending")
    public List<Shop> pendingShops() {
        return shopRepository.findByStatus("PENDING");
    }

    @PostMapping("/shops/{id}/approve")
    public Shop approve(@PathVariable String id) {
        Shop shop = shopRepository.findById(id).orElseThrow();
        shop.setStatus("VERIFIED");
        return shopRepository.save(shop);
    }

    @PostMapping("/shops/{id}/reject")
    public Shop reject(@PathVariable String id) {
        Shop shop = shopRepository.findById(id).orElseThrow();
        shop.setStatus("REJECTED");
        return shopRepository.save(shop);
    }

    @GetMapping("/audit-logs")
    public List<AuditLog> auditLogs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size)).getContent();
    }
}
