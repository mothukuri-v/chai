package com.teasub.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.teasub.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues the short-lived, single-use redemption QR token. The token itself carries no
 * authority beyond "this customer requested a QR at this moment" — every rule (subscription
 * active, not already redeemed today) is re-checked independently at scan time by
 * {@link RedemptionService}, so a stale or tampered token can never bypass business rules.
 */
@Service
public class QrService {

    @Value("${chaipass.qr-jwt-secret}")
    private String secret;

    @Value("${chaipass.qr-token-ttl-seconds}")
    private long ttlSeconds;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public record IssuedToken(String token, String jti, Instant expiresAt) {}

    public IssuedToken issue(String customerId) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(ttlSeconds);

        String token = Jwts.builder()
                .subject(customerId)
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key())
                .compact();

        return new IssuedToken(token, jti, expiry);
    }

    public record DecodedToken(String customerId, String jti) {}

    public DecodedToken verify(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
            return new DecodedToken(claims.getSubject(), claims.getId());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw BusinessException.qrExpired();
        } catch (Exception e) {
            throw BusinessException.qrInvalid();
        }
    }
}
