package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingSessionPortIn;
import com.ban.vehicle_management.application.people.customervehicle.port.in.CustomerVehiclePortIn;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.ai.policy.AiToolRegistry;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class Batch2AiToolPrivacyTest {
    @Mock CurrentAccountPortIn account;
    @Mock SupportTicketPortIn tickets;
    @Mock SubscriptionPortIn subscriptions;
    @Mock CustomerVehiclePortIn vehicles;
    @Mock ParkingSessionPortIn history;
    @Mock KnowledgeRetrievalService retrieval;
    @Mock KnowledgeAccessContextResolver scopes;
    @Spy ObjectMapper mapper = new ObjectMapper();
    @Spy AiToolRegistry registry = new AiToolRegistry();
    @InjectMocks AiToolExecutionService service;

    @Test
    void ownSubscriptionProjectionNeverSerializesCredentialsOrAuditIds() throws Exception {
        Subscription row = new Subscription();
        row.setSubscriptionId(UUID.randomUUID()); row.setCustomerId(UUID.randomUUID());
        row.setQrAccessToken("private-qr-credential"); row.setIdempotencyKey("private-idempotency");
        row.setStatus(SubscriptionStatus.PENDING_SPACE); row.setPrice(new BigDecimal("123456.75"));
        row.setEffectiveFrom(LocalDate.of(2026,11,19)); row.setEffectiveTo(LocalDate.of(2026,12,18));
        when(subscriptions.getMySubscriptions(20)).thenReturn(List.of(row));
        String text = service.executeFromAssistantWorker("get_my_subscription_status", mapper.readTree("{}")).responseJson();
        assertTrue(text.contains("Chờ chọn vị trí")); assertTrue(text.contains("123.456,75"));
        assertTrue(text.contains("19/11/2026"));
        for (String secret : List.of("qrAccessToken", "private-qr", "idempotency", "subscriptionId", row.getCustomerId().toString(), "PENDING_SPACE")) assertFalse(text.contains(secret), secret);
        verify(subscriptions, never()).getSubscriptions(any(),any(),any(),any(),any(),any(),any(),any());
        verify(account).requirePermission("SUBSCRIPTION_READ_OWN");
    }

    @Test
    void vehicleReadMasksPlateAndNeverIncludesCustomerId() throws Exception {
        CustomerVehicle row = new CustomerVehicle(); row.setLicensePlate("59A12345");
        row.setCustomerId(UUID.randomUUID()); row.setStatus(CustomerVehicleStatus.ACTIVE); row.setIsDefault(true);
        when(vehicles.getMyCustomerVehicles(20)).thenReturn(List.of(row));
        String text = service.executeFromAssistantWorker("list_my_vehicles", mapper.readTree("{}")).responseJson();
        assertTrue(text.contains("***345")); assertTrue(text.contains("xe mặc định"));
        assertFalse(text.contains("59A12345")); assertFalse(text.contains(row.getCustomerId().toString()));
        verify(account).requirePermission("CUSTOMER_VEHICLE_READ_OWN");
    }

    @Test
    void historyUsesSummaryQueryWithoutImageUrlResolution() throws Exception {
        when(history.getOwnSessionSummaries(null,20)).thenReturn(List.of());
        String text = service.executeFromAssistantWorker("list_my_parking_history", mapper.readTree("{}")).responseJson();
        assertTrue(text.contains("Chưa thấy lượt gửi xe phù hợp"));
        verify(history).getOwnSessionSummaries(null,20);
        verify(history, never()).getOwnSessions(any(),any(),any(),any(),any(),any(),any(),any());
        verify(account).requirePermission("PARKING_SESSION_READ_OWN");
    }

    @Test
    void revokedPermissionPreventsAnyPersonalQuery() throws Exception {
        doThrow(new AccessDeniedException("revoked")).when(account).requirePermission("PARKING_SESSION_READ_OWN");
        assertThrows(AccessDeniedException.class, () -> service.executeFromAssistantWorker("list_my_parking_history", mapper.readTree("{}")));
        verifyNoInteractions(history, vehicles, subscriptions);
    }

    @Test
    void unadvertisedOwnerOrDateFilterIsRejectedInsteadOfSilentlyIgnored() throws Exception {
        assertThrows(com.ban.vehicle_management.shared.exception.BadRequestException.class,
                () -> service.executeFromAssistantWorker("list_my_vehicles", mapper.readTree("{\"customerId\":\"other\"}")));
        assertThrows(com.ban.vehicle_management.shared.exception.BadRequestException.class,
                () -> service.executeFromAssistantWorker("list_my_parking_history", mapper.readTree("{\"fromDate\":\"2026-11-19\"}")));
        verifyNoInteractions(history, vehicles, subscriptions);
    }

    @Test
    void blockedAccessAndExpiredStatusAreNotMergedIntoActive() {
        Subscription row = new Subscription(); row.setStatus(SubscriptionStatus.EXPIRED);
        assertTrue(AssistantOwnDataFormatter.subscriptions(List.of(row)).contains("Hết hạn"));
        row.setStatus(SubscriptionStatus.ACTIVE); row.setAccessBlocked(true);
        String result = AssistantOwnDataFormatter.subscriptions(List.of(row));
        assertTrue(result.contains("Tạm khóa")); assertFalse(result.contains("Đang hoạt động"));
    }

    @Test
    void boundedResultDoesNotClaimToRepresentEntireHistory() {
        List<Subscription> rows = java.util.stream.IntStream.range(0, 7).mapToObj(i -> new Subscription()).toList();
        String text = AssistantOwnDataFormatter.subscriptions(rows);
        assertTrue(text.contains("tối đa 5 mục")); assertFalse(text.contains("6."));
    }

    @Test
    void ticketReadChecksRequesterEvenWhenUnderlyingReadCouldHaveAdminAccess() throws Exception {
        UUID id = UUID.randomUUID();
        var ticket = new com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket();
        ticket.setRequesterAccountId(UUID.randomUUID()); ticket.setContent("private-content");
        when(tickets.getTicketById(id)).thenReturn(ticket);
        when(account.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        assertThrows(AccessDeniedException.class, () -> service.executeFromAssistantWorker(
                "get_my_support_ticket",mapper.readTree("{\"supportTicketId\":\""+id+"\"}")));
    }

    @Test
    void ticketSummaryDoesNotSerializeRequesterSnapshotsOrFullContent() throws Exception {
        var ticket = new com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket();
        ticket.setTitle("Không đăng ký được vé"); ticket.setRequesterEmailSnapshot("private-example");
        ticket.setContent("private-content"); ticket.setIdempotencyKey("private-key");
        ticket.setStatus(com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus.IN_PROGRESS);
        when(tickets.getMyTickets(null,null,20)).thenReturn(List.of(ticket));
        String text = service.executeFromAssistantWorker("list_my_support_tickets",mapper.readTree("{}")).responseJson();
        assertTrue(text.contains("Đang xử lý"));
        assertFalse(text.contains("private-")); assertFalse(text.contains("IN_PROGRESS"));
    }
}
