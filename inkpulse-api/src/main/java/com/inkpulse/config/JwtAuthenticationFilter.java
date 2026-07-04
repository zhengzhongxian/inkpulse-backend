package com.inkpulse.config;

import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.features.auth.dto.UserSessionDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            var claims = tokenService.parseAccessToken(token);

            String userId = claims.getSubject();
            String username = claims.get("username", String.class);

            if (userId == null || username == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Check if the access token has been blacklisted (e.g. user logged out)
            String jti = claims.getId();
            if (jti != null && tokenService.isTokenBlacklisted(jti)) {
                log.debug("Rejected blacklisted token: jti={}, user={}", jti, userId);
                filterChain.doFilter(request, response);
                return;
            }

            UserSessionDto session = tokenService.getUserSession(UUID.fromString(userId));
            if (session == null) {
                try {
                    session = tokenService.loadAndCacheUserSession(UUID.fromString(userId));
                } catch (Exception e) {
                    log.error("Failed to load user session on cache miss for user: {}", userId, e);
                }
            }

            if (session == null) {
                filterChain.doFilter(request, response);
                return;
            }

            List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
            List<String> roles = session.roles();
            if (roles != null) {
                roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .forEach(authorities::add);
            }
            List<String> permissions = session.permissions();
            if (permissions != null) {
                permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(username);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}

