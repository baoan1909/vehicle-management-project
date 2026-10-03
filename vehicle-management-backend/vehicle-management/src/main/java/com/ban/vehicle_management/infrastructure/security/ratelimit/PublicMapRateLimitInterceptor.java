package com.ban.vehicle_management.infrastructure.security.ratelimit;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PublicMapRateLimitInterceptor implements HandlerInterceptor {

    private final MapApiRateLimiter rateLimiter;
    private final CurrentAccountPortIn currentAccountPortIn;

    public PublicMapRateLimitInterceptor(
            MapApiRateLimiter rateLimiter,
            CurrentAccountPortIn currentAccountPortIn
    ) {
        this.rateLimiter = rateLimiter;
        this.currentAccountPortIn = currentAccountPortIn;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UUID accountId = currentAccountPortIn.getCurrentAccountId().orElse(null);
        rateLimiter.check(request.getRemoteAddr(), accountId);
        return true;
    }
}
