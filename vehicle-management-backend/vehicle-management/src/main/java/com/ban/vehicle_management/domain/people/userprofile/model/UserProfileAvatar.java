package com.ban.vehicle_management.domain.people.userprofile.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileAvatar extends AuditableDomainModel {

    private UUID avatarId;
    private UUID userProfileId;
    private String objectKey;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String checksumSha256;
    private StorageBucket bucket;
    private UserProfileAvatarStatus status;
    private Boolean current;
    private UUID uploadedByAccountId;
}
