package com.teasub.controller;

import com.teasub.service.QrService;
import com.teasub.service.RedemptionService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/redemption")
public class RedemptionController {

    private final RedemptionService redemptionService;

    public RedemptionController(RedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    public record IssueTokenRequest(@NotBlank String customerId) {}
    public record ValidateRequest(@NotBlank String qrToken, @NotBlank String shopOwnerId) {}

    @GetMapping("/today/{customerId}")
    public RedemptionService.TodayStatus today(@PathVariable String customerId) {
        return redemptionService.today(customerId);
    }

    @PostMapping("/issue-token")
    public Map<String, Object> issueToken(@RequestBody IssueTokenRequest req) {
        QrService.IssuedToken issued = redemptionService.issueToken(req.customerId());
        return Map.of("qrToken", issued.token(), "expiresAt", issued.expiresAt().toString());
    }

    @PostMapping("/validate")
    public RedemptionService.RedemptionResult validate(@RequestBody ValidateRequest req) {
        return redemptionService.validate(req.qrToken(), req.shopOwnerId());
    }

    @GetMapping("/customer/{customerId}/history")
    public List<?> history(@PathVariable String customerId) {
        return java.util.Collections.emptyList(); // wired to RedemptionRepository#findByCustomerIdOrderByRedeemedAtDesc in the full build
    }
}
