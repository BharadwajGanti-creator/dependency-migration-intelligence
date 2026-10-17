package com.dmi.api;

import tools.jackson.databind.ObjectMapper;
import com.dmi.domain.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(20)
public class RateLimitFilter extends OncePerRequestFilter {

    private final int requestsPerMinute;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${dmi.rate-limit.requests-per-minute:60}") int requestsPerMinute,
                           ObjectMapper objectMapper) {
        this.requestsPerMinute = requestsPerMinute;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, w) -> {
            if (w == null || now - w.startMs > 60_000) {
                return new Window(now);
            }
            return w;
        });
        if (window.count.incrementAndGet() > requestsPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String traceId = MDC.get("traceId");
            objectMapper.writeValue(response.getOutputStream(),
                    new ApiError("RATE_LIMITED", "Too many requests", traceId != null ? traceId : "unknown", List.of()));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static final class Window {
        final long startMs;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long startMs) {
            this.startMs = startMs;
        }
    }
}
