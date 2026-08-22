package com.teasub.repository;

import com.teasub.model.Subscription;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends MongoRepository<Subscription, String> {
    Optional<Subscription> findFirstByCustomerIdAndStatusOrderByEndDateDesc(String customerId, String status);
    List<Subscription> findByCustomerIdOrderByStartDateDesc(String customerId);
    long countByStatus(String status);
}
