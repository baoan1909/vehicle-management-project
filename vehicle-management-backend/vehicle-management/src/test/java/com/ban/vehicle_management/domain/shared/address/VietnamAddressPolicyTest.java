package com.ban.vehicle_management.domain.shared.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivision;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VietnamAddressPolicyTest {

    @Mock
    private AdministrativeDivisionValidationPortOut validationPort;

    private VietnamAddressPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new VietnamAddressPolicy(validationPort);
    }

    @Test
    void shouldValidateCurrentAddressWhenDistrictCodeIsNull() {
        VietnamAddress address = VietnamAddress.ofCurrent("79", "26734", " 12 Nguyễn Văn A ");
        when(validationPort.findCurrentProvinceByCode("79"))
                .thenReturn(Optional.of(new AdministrativeDivision("79", "Hồ Chí Minh", "Thành phố Hồ Chí Minh")));
        when(validationPort.findCurrentWardByCode("26734"))
                .thenReturn(Optional.of(new AdministrativeDivision("26734", "Bến Nghé", "Phường Bến Nghé")));
        when(validationPort.existsCurrentWardInProvince("26734", "79")).thenReturn(true);

        policy.validateAndBuildDisplay(address);

        assertEquals("12 Nguyễn Văn A, Phường Bến Nghé, Thành phố Hồ Chí Minh", address.getAddressDisplay());
    }

    @Test
    void shouldValidateLegacyHierarchyWhenDistrictCodeIsPresent() {
        VietnamAddress address = VietnamAddress.ofLegacy("79", "760", "26734", "12 Nguyễn Văn A");
        when(validationPort.findLegacyProvinceByCode("79"))
                .thenReturn(Optional.of(new AdministrativeDivision("79", "Hồ Chí Minh", "Thành phố Hồ Chí Minh")));
        when(validationPort.findLegacyDistrictByCode("760"))
                .thenReturn(Optional.of(new AdministrativeDivision("760", "Quận 1", "Quận 1")));
        when(validationPort.findLegacyWardByCode("26734"))
                .thenReturn(Optional.of(new AdministrativeDivision("26734", "Bến Nghé", "Phường Bến Nghé")));
        when(validationPort.existsLegacyDistrictInProvince("760", "79")).thenReturn(true);
        when(validationPort.existsLegacyWardInDistrict("26734", "760")).thenReturn(true);

        policy.validateAndBuildDisplay(address);

        assertEquals(
                "12 Nguyễn Văn A, Phường Bến Nghé, Quận 1, Thành phố Hồ Chí Minh",
                address.getAddressDisplay()
        );
    }

    @Test
    void shouldRejectWardOutsideSelectedProvince() {
        VietnamAddress address = VietnamAddress.ofCurrent("79", "00001", "12 Nguyễn Văn A");
        when(validationPort.findCurrentProvinceByCode("79"))
                .thenReturn(Optional.of(new AdministrativeDivision("79", "Hồ Chí Minh", "Thành phố Hồ Chí Minh")));
        when(validationPort.findCurrentWardByCode("00001"))
                .thenReturn(Optional.of(new AdministrativeDivision("00001", "Phúc Xá", "Phường Phúc Xá")));
        when(validationPort.existsCurrentWardInProvince("00001", "79")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> policy.validateAndBuildDisplay(address));
    }
}
