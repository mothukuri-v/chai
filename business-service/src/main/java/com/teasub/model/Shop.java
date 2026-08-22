package com.teasub.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "shops")
public class Shop {

    @Id
    private String id;

    @Indexed
    private String ownerId;

    private String name;
    private String address;

    @GeoSpatialIndexed(type = org.springframework.data.mongodb.core.index.GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    /** PENDING -> VERIFIED | REJECTED, or SUSPENDED by admin */
    private String status = "PENDING";

    private String qrIdentity;
    private Instant createdAt = Instant.now();

    public boolean isOperational() {
        return "VERIFIED".equals(status);
    }
}
