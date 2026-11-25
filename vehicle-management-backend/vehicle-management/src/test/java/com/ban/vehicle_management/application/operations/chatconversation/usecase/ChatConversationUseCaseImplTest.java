package com.ban.vehicle_management.application.operations.chatconversation.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.ai.port.in.AssistantJobPortIn;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionChatOrder;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatRealtimeEventMapper;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.SubscriptionOrderSnapshotJsonMapper;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.application.operations.supportticket.service.SupportTicketChatMessageContextService;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationMember;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberRole;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatConversationUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private ChatConversationPortOut chatPortOut;

    @Mock
    private ChatRealtimeEventPublisherPortOut realtimeEventPublisher;

    @Mock
    private AssistantJobPortIn assistantJobPortIn;

    @Mock
    private ChatRealtimeEventMapper realtimeEventMapper;

    @Mock
    private SupportTicketChatMessageContextService ticketMessageContextService;

    @Mock
    private SubscriptionOrderSnapshotJsonMapper subscriptionOrderSnapshotJsonMapper;

    @Mock
    private SubscriptionPortOut subscriptionPortOut;

    @Mock
    private OrganizationPortOut organizationPortOut;

    @Mock
    private ParkingLotPortOut parkingLotPortOut;

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private FileAccessPort fileAccessPort;

    @InjectMocks
    private ChatConversationUseCaseImpl chatConversationUseCase;

    @Test
    void customerCanCreateParkingLotConversationWithPermissionScopedPartnerContact() {
        UUID customerAccountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID partnerAccountId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(parkingLotId);
        parkingLot.setName("Bãi xe HCMUTE");
        parkingLot.setStatus(ParkingLotStatus.ACTIVE);
        AtomicReference<ChatConversation> savedConversation = new AtomicReference<>();

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount(customerAccountId, "CUSTOMER"));
        when(chatPortOut.findCustomerIdByAccountId(customerAccountId)).thenReturn(Optional.of(customerId));
        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(parkingLot));
        when(organizationPortOut.findActiveAccountIdsByParkingLotIdWithPermissions(
                parkingLotId,
                Set.of("CHAT_CONVERSATION_READ_OWN", "CHAT_MESSAGE_SEND_OWN", "SUBSCRIPTION_READ_ALL")
        )).thenReturn(Set.of(partnerAccountId));
        when(chatPortOut.findActiveCustomerSupportConversation(customerId, partnerAccountId)).thenReturn(Optional.empty());
        when(chatPortOut.saveConversation(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation conversation = invocation.getArgument(0);
            savedConversation.set(conversation);
            return conversation;
        });
        when(chatPortOut.findAccountIdByCustomerId(customerId)).thenReturn(Optional.of(customerAccountId));
        when(chatPortOut.findMember(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());
        when(chatPortOut.saveMember(any(ChatConversationMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.findConversationById(any(UUID.class))).thenAnswer(invocation -> Optional.of(savedConversation.get()));

        ChatConversation result = chatConversationUseCase.createOrGetParkingLotCustomerConversation(parkingLotId);

        assertEquals(ChatConversationType.CUSTOMER_DIRECT, result.getConversationType());
        assertEquals(partnerAccountId, result.getAssignedTo());
        assertEquals(parkingLotId, result.getRelatedId());
        assertEquals("parking_lots", result.getRelatedTable());
        verify(currentAccountPortIn).requirePermission("CHAT_CONVERSATION_CREATE_OWN");
    }

    @Test
    void customerOrderPickerRequiresOwnSubscriptionReadPermission() {
        UUID conversationId = UUID.randomUUID();
        UUID customerAccountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID partnerAccountId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        ChatConversation conversation = customerPartnerConversation(
                conversationId,
                customerId,
                partnerAccountId
        );

        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(customerAccountId);
        when(chatPortOut.existsActiveMember(conversationId, customerAccountId)).thenReturn(true);
        when(chatPortOut.findCustomerIdByAccountId(customerAccountId)).thenReturn(Optional.of(customerId));
        when(organizationPortOut.findScopedParkingLotIdsByAccountId(partnerAccountId))
                .thenReturn(Set.of(parkingLotId));
        when(subscriptionPortOut.findChatOrders(customerId, Set.of(parkingLotId), 50))
                .thenReturn(List.of());

        assertTrue(chatConversationUseCase.getShareableSubscriptionOrders(conversationId).isEmpty());

        verify(currentAccountPortIn).requirePermission("CHAT_CONVERSATION_READ_OWN");
        verify(currentAccountPortIn).requirePermission("SUBSCRIPTION_READ_OWN");
    }

    @Test
    void partnerOrderPickerRequiresAllSubscriptionReadPermission() {
        UUID conversationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID partnerAccountId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        ChatConversation conversation = customerPartnerConversation(
                conversationId,
                customerId,
                partnerAccountId
        );

        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(partnerAccountId);
        when(chatPortOut.existsActiveMember(conversationId, partnerAccountId)).thenReturn(true);
        when(chatPortOut.findCustomerIdByAccountId(partnerAccountId)).thenReturn(Optional.empty());
        when(organizationPortOut.findScopedParkingLotIdsByAccountId(partnerAccountId))
                .thenReturn(Set.of(parkingLotId));
        when(subscriptionPortOut.findChatOrders(customerId, Set.of(parkingLotId), 50))
                .thenReturn(List.of());

        assertTrue(chatConversationUseCase.getShareableSubscriptionOrders(conversationId).isEmpty());

        verify(currentAccountPortIn).requirePermission("CHAT_CONVERSATION_READ_OWN");
        verify(currentAccountPortIn).requirePermission("SUBSCRIPTION_READ_ALL");
    }

    @Test
    void customerCanShareSubscriptionOrderAsContextCardAfterExplicitAction() {
        UUID conversationId = UUID.randomUUID();
        UUID customerAccountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID partnerAccountId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        ChatConversation conversation = customerPartnerConversation(conversationId, customerId, partnerAccountId);
        SubscriptionChatOrder order = new SubscriptionChatOrder(
                subscriptionId,
                parkingLotId,
                "HCMUTE",
                "Bãi xe HCMUTE",
                "MONTHLY",
                "Vé tháng",
                "123456H",
                "Xe máy",
                LocalDate.of(2026, 11, 4),
                LocalDate.of(2026, 12, 3),
                new BigDecimal("80000"),
                SubscriptionStatus.ACTIVE,
                Instant.parse("2026-11-04T00:27:59Z")
        );

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(customerAccountId);
        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(chatPortOut.existsActiveMember(conversationId, customerAccountId)).thenReturn(true);
        when(chatPortOut.findCustomerIdByAccountId(customerAccountId)).thenReturn(Optional.of(customerId));
        when(organizationPortOut.findScopedParkingLotIdsByAccountId(partnerAccountId))
                .thenReturn(Set.of(parkingLotId));
        when(subscriptionPortOut.findChatOrderById(subscriptionId, customerId, Set.of(parkingLotId)))
                .thenReturn(Optional.of(order));
        when(subscriptionOrderSnapshotJsonMapper.toJson(any(SubscriptionChatOrder.class), any(Instant.class)))
                .thenReturn("{\"schemaVersion\":1}");
        when(chatPortOut.saveMessage(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(conversation)).thenReturn(conversation);

        ChatMessage result = chatConversationUseCase.shareSubscriptionOrder(conversationId, subscriptionId);

        assertEquals(ChatMessageType.CONTEXT_CARD, result.getMessageType());
        assertEquals(subscriptionId, result.getRelatedId());
        assertEquals("access_control", result.getRelatedSchema());
        assertEquals("subscriptions", result.getRelatedTable());
        assertEquals("{\"schemaVersion\":1}", result.getMetadata());
        verify(currentAccountPortIn).requirePermission("CHAT_MESSAGE_SEND_OWN");
        verify(currentAccountPortIn).requirePermission("SUBSCRIPTION_READ_OWN");
    }

    @Test
    void orderPickerRejectsLegacyConversationWithoutCustomerIdentityAsBusinessError() {
        UUID conversationId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        ChatConversation conversation = customerPartnerConversation(conversationId, null, UUID.randomUUID());

        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(chatPortOut.existsActiveMember(conversationId, accountId)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> chatConversationUseCase.getShareableSubscriptionOrders(conversationId));

        verify(subscriptionPortOut, never()).findChatOrders(
                any(), any(), org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void staleFinalTicketContextIsPersistedAsGenericPrivateChatMessage() {
        UUID accountId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID staleTicketId = UUID.randomUUID();
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(conversationId);
        conversation.setConversationType(ChatConversationType.SUPPORT_REQUESTER_DIRECT);
        conversation.setStatus(ChatConversationStatus.ACTIVE);

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(chatPortOut.existsActiveMember(conversationId, accountId)).thenReturn(true);
        when(ticketMessageContextService.resolveContextForSend(conversation, staleTicketId, accountId)).thenReturn(null);
        when(chatPortOut.saveMessage(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(conversation)).thenReturn(conversation);

        ChatMessage result = chatConversationUseCase.sendTextMessage(
                conversationId, "Trao đổi chăm sóc chung", null, staleTicketId
        );

        assertNull(result.getContextTicketId());
        verify(chatPortOut).saveMessage(argThat(message -> message.getContextTicketId() == null));
        verify(ticketMessageContextService).resolveContextForSend(conversation, staleTicketId, accountId);
    }

    @ParameterizedTest(name = "{0} can create a direct conversation with a selected customer")
    @ValueSource(strings = {"PARTNER_ADMIN", "PARKING_MANAGER", "EMPLOYEE"})
    void shouldCreateCustomerConversationWithBothSidesAsActiveMembers(String roleCode) {
        UUID employeeAccountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID customerAccountId = UUID.randomUUID();
        AtomicReference<ChatConversation> savedConversation = new AtomicReference<>();

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount(employeeAccountId, roleCode));
        when(chatPortOut.existsCustomer(customerId)).thenReturn(true);
        when(chatPortOut.findActiveCustomerSupportConversation(customerId, employeeAccountId)).thenReturn(Optional.empty());
        when(chatPortOut.saveConversation(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation conversation = invocation.getArgument(0);
            savedConversation.set(conversation);
            return conversation;
        });
        when(chatPortOut.findAccountIdByCustomerId(customerId)).thenReturn(Optional.of(customerAccountId));
        when(chatPortOut.findMember(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());
        when(chatPortOut.saveMember(any(ChatConversationMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.findConversationById(any(UUID.class))).thenAnswer(invocation -> Optional.of(savedConversation.get()));

        ChatConversation result = chatConversationUseCase.createOrGetCustomerSupportConversation(customerId, "Hỗ trợ khách hàng");

        assertEquals(savedConversation.get().getConversationId(), result.getConversationId());
        ArgumentCaptor<ChatConversationMember> memberCaptor = ArgumentCaptor.forClass(ChatConversationMember.class);
        verify(chatPortOut, org.mockito.Mockito.times(2)).saveMember(memberCaptor.capture());
        List<ChatConversationMember> savedMembers = new ArrayList<>(memberCaptor.getAllValues());
        assertTrue(savedMembers.stream().anyMatch(member -> employeeAccountId.equals(member.getAccountId())
                && member.getMemberRole() == ChatMemberRole.OWNER));
        assertTrue(savedMembers.stream().anyMatch(member -> customerAccountId.equals(member.getAccountId())
                && member.getMemberRole() == ChatMemberRole.CUSTOMER));
        verify(currentAccountPortIn).requirePermission("CHAT_CONVERSATION_CREATE_CUSTOMER_DIRECT");
        verify(chatPortOut, never()).findCustomerIdByAccountId(employeeAccountId);
        verify(currentAccountPortIn, never()).requirePermission("CHAT_CONVERSATION_CREATE_OWN");
    }

    @Test
    void shouldRejectAddingMemberToAssistantSupportConversation() {
        UUID conversationId = UUID.randomUUID();
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(conversationId);
        conversation.setConversationType(ChatConversationType.ASSISTANT_SUPPORT);
        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));

        assertThrows(BadRequestException.class,
                () -> chatConversationUseCase.addMember(conversationId, UUID.randomUUID()));

        verify(chatPortOut, never()).saveMember(any(ChatConversationMember.class));
    }

    @Test
    void shouldRejectRemovingMemberFromCustomerDirectConversation() {
        UUID conversationId = UUID.randomUUID();
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(conversationId);
        conversation.setConversationType(ChatConversationType.CUSTOMER_DIRECT);
        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));

        assertThrows(BadRequestException.class,
                () -> chatConversationUseCase.removeMember(conversationId, UUID.randomUUID()));

        verify(chatPortOut, never()).removeMember(any(), any(), any());
    }

    private ChatConversation customerPartnerConversation(
            UUID conversationId,
            UUID customerId,
            UUID partnerAccountId
    ) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(conversationId);
        conversation.setConversationType(ChatConversationType.CUSTOMER_DIRECT);
        conversation.setStatus(ChatConversationStatus.ACTIVE);
        conversation.setCustomerId(customerId);
        conversation.setOwnerAccountId(partnerAccountId);
        conversation.setAssignedTo(partnerAccountId);
        return conversation;
    }

    private CurrentAccountAccess currentAccount(UUID accountId, String roleCode) {
        return new CurrentAccountAccess(
                accountId,
                "subject",
                "employee",
                "employee@example.com",
                UUID.randomUUID(),
                roleCode,
                null,
                null,
                null,
                null,
                java.util.Set.of()
        );
    }
}
