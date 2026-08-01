package com.krushna.moviebooking.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Gateway JWT authentication filter.
 * Validates Bearer token on every inbound request and populates SecurityContext.
 * Also injects X-User-Id / X-User-Email / X-User-Roles headers for downstream services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayJwtFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String jwt = extractBearerToken(request);

        if (StringUtils.hasText(jwt) && jwtValidator.isValid(jwt)) {
            try {
                String userId = jwtValidator.getUserId(jwt).toString();
                String email = jwtValidator.getEmail(jwt);
                List<String> roles = jwtValidator.getRoles(jwt);

                List<SimpleGrantedAuthority> authorities = roles != null
                        ? roles.stream().map(SimpleGrantedAuthority::new).toList()
                        : List.of();

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("[Gateway] Authenticated userId={} email={}", userId, email);
            } catch (Exception ex) {
                log.error("[Gateway] Failed to set auth from token: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
