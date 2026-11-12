package com.ban.vehicle_management.infrastructure.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.AccountDeletionPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.infrastructure.config.JacksonConfig;
import com.ban.vehicle_management.infrastructure.config.time.ApplicationTimeProperties;
import com.ban.vehicle_management.infrastructure.security.principal.AuthenticatedAccountPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AccountDeletionLifecycleFilterTest {

    private static final Instant AUTHENTICATED_AT = Instant.parse("2026-11-12T08:13:05Z");

    @Mock
    private AccountDeletionPortIn useCase;

    @Mock
    private AccountDeletionPortOut persistence;

    private AccountDeletionLifecycleFilter filter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new JacksonConfig()
                .objectMapper(new ApplicationTimeProperties("Asia/Ho_Chi_Minh"));
        filter = new AccountDeletionLifecycleFilter(useCase, persistence, objectMapper);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesAccountFromAuthenticatedPrincipalWhenJwtHasNoAccountIdClaim() throws Exception {
        UUID accountId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(authentication(accountId));
        when(persistence.isDeletedAccount(accountId)).thenReturn(false);
        when(useCase.cancelOnFreshLogin(accountId, AUTHENTICATED_AT)).thenReturn(Optional.empty());
        when(persistence.findActive(accountId, false)).thenReturn(Optional.empty());
        MockHttpServletRequest request = applicationRequest("GET", "/api/iam/accounts/current-access");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(useCase).cancelOnFreshLogin(accountId, AUTHENTICATED_AT);
        assertNotNull(chain.getRequest());
    }

    @Test
    void blocksPendingAccountWhenApplicationUsesContextPath() throws Exception {
        UUID accountId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication(accountId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(persistence.isDeletedAccount(accountId)).thenReturn(false);
        when(useCase.cancelOnFreshLogin(accountId, AUTHENTICATED_AT)).thenReturn(Optional.empty());
        when(persistence.findActive(accountId, false))
                .thenReturn(Optional.of(mock(AccountDeletionRequest.class)));
        MockHttpServletRequest request = applicationRequest("GET", "/api/iam/accounts/current-access");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(423, response.getStatus());
        assertEquals("ACCOUNT_PENDING_DELETION", response.getHeader("X-Auth-Error-Code"));
        assertNull(chain.getRequest());
    }

    @Test
    void allowsDeletionManagementRequestWhenApplicationUsesContextPath() throws Exception {
        UUID accountId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(authentication(accountId));
        when(persistence.isDeletedAccount(accountId)).thenReturn(false);
        when(useCase.cancelOnFreshLogin(accountId, AUTHENTICATED_AT)).thenReturn(Optional.empty());
        when(persistence.findActive(accountId, false))
                .thenReturn(Optional.of(mock(AccountDeletionRequest.class)));
        MockHttpServletRequest request = applicationRequest(
                "GET", "/api/iam/account-deletion-requests/current");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    private JwtAuthenticationToken authentication(UUID accountId) {
        Jwt jwt = Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .subject("keycloak-user-id")
                .claim("auth_time", AUTHENTICATED_AT.getEpochSecond())
                .issuedAt(AUTHENTICATED_AT)
                .expiresAt(AUTHENTICATED_AT.plusSeconds(300))
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of(), "customer");
        authentication.setDetails(new AuthenticatedAccountPrincipal(
                accountId, "keycloak-user-id", "customer", "customer@example.com"));
        return authentication;
    }

    private MockHttpServletRequest applicationRequest(String method, String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest(
                method, "/vehicle-management" + servletPath);
        request.setContextPath("/vehicle-management");
        request.setServletPath(servletPath);
        request.setRequestURI("/vehicle-management" + servletPath);
        return request;
    }
}
