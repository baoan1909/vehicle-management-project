package com.ban.vehicle_management.infrastructure.security.jwt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationConverterTest {

    @Mock
    private AccountAuthorizationPortOut accountAuthorizationPortOut;

    @InjectMocks
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void shouldNotGrantBusinessPermissionsToPendingInternalEmployee() {
        UUID accountId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject-001")
                .claim("account_id", accountId.toString())
                .claim("preferred_username", "pending.employee")
                .claim("email", "pending.employee@example.com")
                .build();
        when(accountAuthorizationPortOut.findByAccountId(accountId)).thenReturn(java.util.Optional.of(
                new CurrentAccountAccess(
                        accountId,
                        "subject-001",
                        "pending.employee",
                        "pending.employee@example.com",
                        UUID.randomUUID(),
                        "EMPLOYEE",
                        AccountStatus.ACTIVE,
                        EmployeeStatus.INACTIVE,
                        Set.of("EMPLOYEE_READ_ALL")
                )
        ));

        JwtAuthenticationToken authenticationToken = (JwtAuthenticationToken) jwtAuthenticationConverter.convert(jwt);

        assertFalse(authenticationToken.getAuthorities().stream()
                .anyMatch(authority -> "EMPLOYEE_READ_ALL".equals(authority.getAuthority())));
    }

    @Test
    void shouldGrantBusinessPermissionsToApprovedInternalEmployee() {
        UUID accountId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject-002")
                .claim("account_id", accountId.toString())
                .claim("preferred_username", "approved.employee")
                .claim("email", "approved.employee@example.com")
                .build();
        when(accountAuthorizationPortOut.findByAccountId(accountId)).thenReturn(java.util.Optional.of(
                new CurrentAccountAccess(
                        accountId,
                        "subject-002",
                        "approved.employee",
                        "approved.employee@example.com",
                        UUID.randomUUID(),
                        "EMPLOYEE",
                        AccountStatus.ACTIVE,
                        EmployeeStatus.ACTIVE,
                        Set.of("EMPLOYEE_READ_ALL")
                )
        ));

        JwtAuthenticationToken authenticationToken = (JwtAuthenticationToken) jwtAuthenticationConverter.convert(jwt);

        assertTrue(authenticationToken.getAuthorities().stream()
                .anyMatch(authority -> "EMPLOYEE_READ_ALL".equals(authority.getAuthority())));
    }
}
