package com.ban.vehicle_management.domain.notification.userguide.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideVersion extends AuditableDomainModel {

    private UUID guideVersionId;
    private UUID guideId;
    private Integer versionNumber;
    private String title;
    private String summary;
    private String deepLink;
    private UserGuideAudienceMode audienceMode;
    private Instant publishedAt;
    private UUID publishedBy;
    private List<UserGuideStep> steps = new ArrayList<>();
    private List<UserGuideAudience> audiences = new ArrayList<>();

    public List<UserGuideStep> getSteps() {
        return Collections.unmodifiableList(steps == null ? List.of() : new ArrayList<>(steps));
    }

    public void setSteps(List<UserGuideStep> steps) {
        this.steps = steps == null ? new ArrayList<>() : new ArrayList<>(steps);
    }

    public List<UserGuideAudience> getAudiences() {
        return Collections.unmodifiableList(audiences == null ? List.of() : new ArrayList<>(audiences));
    }

    public void setAudiences(List<UserGuideAudience> audiences) {
        this.audiences = audiences == null ? new ArrayList<>() : new ArrayList<>(audiences);
    }
}
