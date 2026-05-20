package com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LostCardReport extends AuditableDomainModel {

    private UUID lostCardReportId;
    private UUID cardId;
    private UUID customerId;
    private UUID parkingSessionId;
    private Instant notificationTime;
    private Instant timeOfLost;
    private BigDecimal ticketPrice;
    private BigDecimal lostCardFee;
    private String reporterName;
    private String reporterPhone;
    private String identifyCard;
    private String registrationLicense;
    private String note;
    private LostCardReportStatus status;
    private UUID resolvedBy;
    private Instant resolvedAt;
}

