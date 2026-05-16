package com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request;

import com.ban.vehicle_management.shared.enumeration.CardStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChangeCardStatusRequest {

    private CardStatus status;
    private String blockedReason;
}
