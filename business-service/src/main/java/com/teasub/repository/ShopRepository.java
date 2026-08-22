package com.teasub.repository;

import com.teasub.model.Shop;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ShopRepository extends MongoRepository<Shop, String> {
    List<Shop> findByOwnerId(String ownerId);
    List<Shop> findByStatus(String status);

    @Query("{ 'status': 'VERIFIED', 'location': { $near: { $geometry: { type: 'Point', coordinates: [?1, ?0] }, $maxDistance: ?2 } } }")
    List<Shop> findNearby(double lat, double lng, double maxDistanceMeters);
}
