package com.krushna.moviebooking.gateway.filter;

import com.krushna.moviebooking.gateway.redis.GatewayRedisRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Gateway filter that enforces Redis-backed rate limiting per authenticated user or client IP.
 *
 * <p>Identifies clients by:
 * <ul>
 *   <li>{@code user:{userId}} for authenticated requests</li>
 *   <li>{@code ip:{clientIp}} for unauthenticated/anonymous requests</li>
 * </ul>
 *
 * <p>Excludes internal health checks, metrics, and API documentation endpoints.
 * Returns HTTP 429 Too Many Requests with {@code Retry-After: 60} header when limits are exceeded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final GatewayRedisRepository gatewayRedisRepository;

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui",
            "/fallback",
            "/gateway/health"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);

        boolean allowed;
        try {
            allowed = gatewayRedisRepository.isWithinRateLimit(clientKey);
        } catch (Exception e) {
            log.warn("[RateLimitingFilter] Redis error while checking rate limit for key={}: {}. Allowing request.",
                    clientKey, e.getMessage());
            allowed = true;
        }

        if (!allowed) {
            log.warn("[RateLimitingFilter] Throttling request to path='{}' for clientKey='{}'", path, clientKey);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests. Please try again later.\"}}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExcluded(String path) {
        if (path == null) {
            return false;
        }
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String resolveClientKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getPrincipal();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String clientIp = forwardedFor.split(",")[0].trim();
            if (StringUtils.hasText(clientIp)) {
                return "ip:" + clientIp;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return "ip:" + (StringUtils.hasText(remoteAddr) ? remoteAddr : "unknown");
    }
}
