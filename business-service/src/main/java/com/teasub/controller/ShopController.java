package com.teasub.controller;

import com.teasub.model.Shop;
import com.teasub.repository.RedemptionRepository;
import com.teasub.repository.ShopRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/shops")
public class ShopController {

    private final ShopRepository shopRepository;
    private final RedemptionRepository redemptionRepository;

    public ShopController(ShopRepository shopRepository, RedemptionRepository redemptionRepository) {
        this.shopRepository = shopRepository;
        this.redemptionRepository = redemptionRepository;
    }

    public record CreateShopRequest(@NotBlank String ownerId, @NotBlank String name, String address, Double lat, Double lng) {}

    @PostMapping
    public Shop create(@RequestBody CreateShopRequest req) {
        Shop shop = new Shop();
        shop.setOwnerId(req.ownerId());
        shop.setName(req.name());
        shop.setAddress(req.address());
        if (req.lat() != null && req.lng() != null) {
            shop.setLocation(new GeoJsonPoint(req.lng(), req.lat()));
        }
        shop.setStatus("PENDING");
        shop.setQrIdentity("SHOP-" + UUID.randomUUID());
        return shopRepository.save(shop);
    }

    @GetMapping("/owner/{ownerId}")
    public List<Shop> mine(@PathVariable String ownerId) {
        return shopRepository.findByOwnerId(ownerId);
    }

    @GetMapping("/nearby")
    public List<Shop> nearby(@RequestParam double lat, @RequestParam double lng, @RequestParam(defaultValue = "5") double radiusKm) {
        return shopRepository.findNearby(lat, lng, radiusKm * 1000);
    }

    @GetMapping("/owner/{ownerId}/redemptions/today")
    public Map<String, Object> todaysRedemptions(@PathVariable String ownerId) {
        Shop shop = shopRepository.findByOwnerId(ownerId).stream().findFirst().orElseThrow();
        String today = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();
        long count = redemptionRepository.countByShopIdAndRedemptionDate(shop.getId(), today);
        return Map.of("shopId", shop.getId(), "date", today, "count", count);
    }

    @GetMapping("/owner/{ownerId}/analytics")
    public Map<String, Object> analytics(@PathVariable String ownerId) {
        // In the full build this aggregates redemptions over the last 30 days via an
        // aggregation pipeline; kept minimal here to keep the sample focused.
        Shop shop = shopRepository.findByOwnerId(ownerId).stream().findFirst().orElseThrow();
        return Map.of("shopId", shop.getId(), "shopName", shop.getName(), "status", shop.getStatus());
    }
}
