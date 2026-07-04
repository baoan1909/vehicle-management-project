package com.ban.vehicle_management.infrastructure.persistence.database.entity.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profile_avatars", schema = "people")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileAvatarEntity extends AuditableEntity {

    @Id
    @Column(name = "avatar_id", nullable = false)
    private UUID avatarId;

    @Column(name = "user_profile_id", nullable = false)
    private UUID userProfileId;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "checksum_sha256")
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "bucket", nullable = false)
    private StorageBucket bucket;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserProfileAvatarStatus status;

    @Column(name = "is_current", nullable = false)
    private Boolean current;

    @Column(name = "uploaded_by_account_id")
    private UUID uploadedByAccountId;
}
