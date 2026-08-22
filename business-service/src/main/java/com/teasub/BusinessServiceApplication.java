package com.teasub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ChaiPass Business Service — owns every rule that must never be wrong:
 * subscription validity, one-tea-per-day enforcement, QR authenticity and
 * payment reconciliation. Reachable only from the Node.js API gateway
 * (no public ingress — see infra/docker-compose.yml network config).
 */
@SpringBootApplication
public class BusinessServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusinessServiceApplication.class, args);
    }
}
