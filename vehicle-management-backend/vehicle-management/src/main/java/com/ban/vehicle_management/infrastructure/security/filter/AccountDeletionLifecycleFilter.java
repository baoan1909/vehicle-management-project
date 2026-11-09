package com.ban.vehicle_management.infrastructure.security.filter;

import com.ban.vehicle_management.application.iam.account.port.in.AccountDeletionPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AccountDeletionLifecycleFilter extends OncePerRequestFilter {

    private static final String DELETION_BASE_PATH = "/api/iam/account-deletion-requests";
    private static final String DELETION_CURRENT_PATH = DELETION_BASE_PATH + "/current";
    private final AccountDeletionPortIn useCase;
    private final AccountDeletionPortOut persistence;
    private final ObjectMapper objectMapper;

    public AccountDeletionLifecycleFilter(AccountDeletionPortIn useCase,
            AccountDeletionPortOut persistence,
            ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.persistence = persistence;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || (!path.startsWith("/api/") && !path.startsWith("/ws"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        Optional<UUID> accountId = parseUuid(jwt.getToken().getClaim("account_id"));
        if (accountId.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (persistence.isDeletedAccount(accountId.get())) {
            write(response, HttpStatus.UNAUTHORIZED, "Tài khoản đã bị xóa", "ACCOUNT_DELETED");
            return;
        }

        Instant authenticatedAt = parseInstant(jwt.getToken().getClaim("auth_time"));
        useCase.cancelOnFreshLogin(accountId.get(), authenticatedAt);
        boolean pending = persistence.findActive(accountId.get(), false).isPresent();
        if (!pending || isDeletionManagementRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        write(response, HttpStatus.LOCKED, "Tài khoản đang trong thời gian chờ xóa", "ACCOUNT_PENDING_DELETION");
    }

    private boolean isDeletionManagementRequest(HttpServletRequest request) {
        return (DELETION_BASE_PATH.equals(request.getRequestURI()) && HttpMethod.POST.matches(request.getMethod()))
                || (DELETION_CURRENT_PATH.equals(request.getRequestURI())
                && (HttpMethod.GET.matches(request.getMethod()) || HttpMethod.DELETE.matches(request.getMethod())));
    }

    private Optional<UUID> parseUuid(Object value) {
        if (value instanceof UUID uuid) return Optional.of(uuid);
        if (value instanceof String text) {
            try { return Optional.of(UUID.fromString(text)); }
            catch (IllegalArgumentException ignored) { return Optional.empty(); }
        }
        return Optional.empty();
    }

    private Instant parseInstant(Object value) {
        if (value instanceof Instant instant) return instant;
        if (value instanceof Number number) return Instant.ofEpochSecond(number.longValue());
        if (value instanceof String text) {
            try { return Instant.ofEpochSecond(Long.parseLong(text)); }
            catch (NumberFormatException ignored) {
                try { return Instant.parse(text); }
                catch (RuntimeException ignoredAgain) { return null; }
            }
        }
        return null;
    }

    private void write(HttpServletResponse response, HttpStatus status, String message, String code)
            throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-Auth-Error-Code", code);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(message));
    }
}
