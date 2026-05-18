package com.ban.vehicle_management.application.iam.role.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleUseCaseImplTest {

    @Mock
    private RolePortOut rolePortOut;

    @InjectMocks
    private RoleUseCaseImpl roleUseCase;

    @Test
    void shouldCreateRoleUsingSharedTextValidationRules() {
        Role requestRole = new Role();
        requestRole.setCode(" admin-role ");
        requestRole.setName(" Administrator ");
        requestRole.setDescription(" Core system role ");

        when(rolePortOut.existsByCode("ADMIN-ROLE")).thenReturn(false);
        when(rolePortOut.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role createdRole = roleUseCase.createRole(requestRole);

        assertEquals("ADMIN-ROLE", createdRole.getCode());
        assertEquals("Administrator", createdRole.getName());
        assertEquals("Core system role", createdRole.getDescription());
        assertTrue(createdRole.getIsActive());
        assertEquals(Boolean.FALSE, createdRole.getIsSystem());
    }

    @Test
    void shouldReturnConflictWhenRoleCodeAlreadyExists() {
        Role requestRole = new Role();
        requestRole.setCode("ADMIN");
        requestRole.setName("Administrator");

        when(rolePortOut.existsByCode("ADMIN")).thenReturn(true);

        assertThrows(ConflictException.class, () -> roleUseCase.createRole(requestRole));
        verify(rolePortOut, never()).save(any(Role.class));
    }

    @Test
    void shouldNormalizeBlankKeywordToNullWhenListingRoles() {
        roleUseCase.getRoles(Boolean.TRUE, Boolean.FALSE, "   ");

        verify(rolePortOut).findAll(Boolean.TRUE, Boolean.FALSE, null);
    }
}
