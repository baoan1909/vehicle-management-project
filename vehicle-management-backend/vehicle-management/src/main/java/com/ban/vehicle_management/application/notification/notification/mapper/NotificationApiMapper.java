package com.ban.vehicle_management.application.notification.notification.mapper;

import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.entrypoint.dto.notification.notification.response.NotificationUserResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.notification.response.NotificationUnreadCountResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.notification.response.NotificationActiveRoleResponse;
import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationApiMapper {

    NotificationUserResponse toUserResponse(Notification notification);

    List<NotificationUserResponse> toUserResponses(List<Notification> notifications);

    NotificationUnreadCountResponse toUnreadCountResponse(Long unreadCount);

    NotificationActiveRoleResponse toActiveRoleResponse(Role role);

    List<NotificationActiveRoleResponse> toActiveRoleResponses(List<Role> roles);

}
