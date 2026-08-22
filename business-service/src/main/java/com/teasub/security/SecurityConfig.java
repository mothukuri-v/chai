package com.teasub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final ServiceTokenFilter serviceTokenFilter;

    public SecurityConfig(ServiceTokenFilter serviceTokenFilter) {
        this.serviceTokenFilter = serviceTokenFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // stateless, internal, token-authenticated service — no browser session/cookies
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/internal/actuator/health").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(serviceTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
