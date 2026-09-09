package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountIdentity extends AuditableDomainModel {

    private UUID accountIdentityId;
    private UUID accountId;
    private SocialIdentityProvider provider;
    private String providerSubject;
    private String providerUsername;
    private String providerEmail;
}
