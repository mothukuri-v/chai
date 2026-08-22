package com.teasub.repository;

import com.teasub.model.Redemption;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RedemptionRepository extends MongoRepository<Redemption, String> {
    Optional<Redemption> findByCustomerIdAndRedemptionDate(String customerId, String redemptionDate);
    boolean existsByCustomerIdAndRedemptionDate(String customerId, String redemptionDate);
    List<Redemption> findByCustomerIdOrderByRedeemedAtDesc(String customerId);
    long countByShopIdAndRedemptionDate(String shopId, String redemptionDate);
    long countByRedemptionDate(String redemptionDate);
}
