package com.teasub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Every request that reaches this service must carry a short-lived (30s) X-Service-Token
 * minted by the Node.js gateway. This is NOT a substitute for the gateway's user-facing auth —
 * it is defence in depth so this service never trusts network position alone (e.g. if the
 * network boundary is ever misconfigured, requests still fail closed without a valid token).
 */
@Component
public class ServiceTokenFilter extends OncePerRequestFilter {

    @Value("${chaipass.service-jwt-secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/internal/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("X-Service-Token");
        if (token == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing service token");
            return;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!"business-service".equals(claims.get("aud"))) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token audience");
                return;
            }

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(request, response);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired service token");
        }
    }
}
