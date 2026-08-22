package com.teasub.config;

import com.mongodb.client.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * Multi-document transactions (used by RedemptionService to write the redemption and
 * decrement the subscription credit atomically) require MongoDB to run as a replica set.
 * infra/docker-compose.yml starts Mongo with --replSet rs0 and runs rs.initiate() for
 * exactly this reason.
 */
@Configuration
public class MongoConfig {

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
