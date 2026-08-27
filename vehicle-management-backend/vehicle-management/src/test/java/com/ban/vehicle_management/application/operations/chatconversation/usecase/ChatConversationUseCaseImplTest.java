package com.ban.vehicle_management.application.operations.chatconversation.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatRealtimeEventMapper;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationMember;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private ChatRealtimeEventMapper realtimeEventMapper;

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private FileAccessPort fileAccessPort;

    @InjectMocks
    private ChatConversationUseCaseImpl chatConversationUseCase;

    @Test
    void shouldCreateCustomerConversationWithBothSidesAsActiveMembers() {
        UUID employeeAccountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID customerAccountId = UUID.randomUUID();
        AtomicReference<ChatConversation> savedConversation = new AtomicReference<>();

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount(employeeAccountId));
        when(currentAccountPortIn.hasPermission(anyString())).thenAnswer(invocation ->
                "CHAT_CONVERSATION_CREATE_ALL".equals(invocation.getArgument(0))
        );
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
        verify(currentAccountPortIn, never()).requirePermission("CHAT_CONVERSATION_CREATE_OWN");
    }

    private CurrentAccountAccess currentAccount(UUID accountId) {
        return new CurrentAccountAccess(
                accountId,
                "subject",
                "employee",
                "employee@example.com",
                UUID.randomUUID(),
                "EMPLOYEE",
                null,
                null,
                null,
                null,
                java.util.Set.of()
        );
    }
}
