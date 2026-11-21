package com.ban.vehicle_management.entrypoint.parking;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ban.vehicle_management.application.iam.account.port.in.AccountDeletionPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.billing.payment.port.in.SepayPaymentPortIn;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.mapper.ParkingSessionPrepaymentApiMapper;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.port.in.ParkingSessionPrepaymentPortIn;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.usecase.GuestParkingSessionPrepaymentCheckoutUseCase;
import com.ban.vehicle_management.entrypoint.controller.parking.PublicParkingSessionPrepaymentController;
import com.ban.vehicle_management.infrastructure.security.config.SecurityConfig;
import com.ban.vehicle_management.infrastructure.security.config.SecurityTimeProperties;
import com.ban.vehicle_management.infrastructure.security.filter.AccountDeletionLifecycleFilter;
import com.ban.vehicle_management.infrastructure.security.jwt.JwtAuthenticationConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks")
@ContextConfiguration(classes = {
        PublicParkingSessionPrepaymentController.class,
        SecurityConfig.class,
        SecurityTimeProperties.class,
        AccountDeletionLifecycleFilter.class,
        PublicParkingSessionPrepaymentSecurityTest.WebSecurityTestConfiguration.class
})
class PublicParkingSessionPrepaymentSecurityTest {

    private static final String BASE_PATH = "/api/public/parking/prepayments";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParkingSessionPrepaymentPortIn prepayments;
    @MockitoBean
    private GuestParkingSessionPrepaymentCheckoutUseCase checkout;
    @MockitoBean
    private ParkingSessionPrepaymentApiMapper mapper;
    @MockitoBean
    private SepayPaymentPortIn sepay;
    @MockitoBean
    private JwtAuthenticationConverter jwtAuthenticationConverter;
    @MockitoBean
    private AccountDeletionPortIn accountDeletion;
    @MockitoBean
    private AccountDeletionPortOut accountDeletionPersistence;
    @MockitoBean
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @CsvSource({
            "'/lookup', lookup",
            "'', create",
            "'/00000000-0000-0000-0000-000000000001/status', status",
            "'/00000000-0000-0000-0000-000000000001/qr', downloadQr"
    })
    void shouldValidateGuestRequestsWithoutAuthentication(String pathSuffix, String methodName) throws Exception {
        mockMvc.perform(post(BASE_PATH + pathSuffix)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(handler().handlerType(PublicParkingSessionPrepaymentController.class))
                .andExpect(handler().methodName(methodName));
    }

    @Test
    void shouldReturnDownloadablePngDataAfterGuestCredentialsAreChecked() throws Exception {
        UUID id = UUID.randomUUID();
        when(sepay.downloadParkingSessionPrepaymentQr(id, "60A99999", "V018"))
                .thenReturn(new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47});

        mockMvc.perform(post(BASE_PATH + "/" + id + "/qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"licensePlate":"60A99999","cardNumber":"V018"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("iVBORw=="));
    }

    @Test
    void shouldRequireAuthenticationOutsideTheAllowedPrepaymentRoutes() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/lookup"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE_PATH + "/other"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class WebSecurityTestConfiguration {
    }
}
