import type { NotificationUserResponse } from "@/features/notifications/api/notificationApi";

const NOTIFICATION_RECEIVED_EVENT = "vehicle-management:notification-received";

export function publishNotificationReceived(notification: NotificationUserResponse) {
  window.dispatchEvent(new CustomEvent<NotificationUserResponse>(NOTIFICATION_RECEIVED_EVENT, { detail: notification }));
}

export function subscribeNotificationReceived(
  listener: (notification: NotificationUserResponse) => void,
) {
  const eventListener = (event: Event) => {
    listener((event as CustomEvent<NotificationUserResponse>).detail);
  };
  window.addEventListener(NOTIFICATION_RECEIVED_EVENT, eventListener);
  return () => window.removeEventListener(NOTIFICATION_RECEIVED_EVENT, eventListener);
}
