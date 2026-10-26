import { Client } from "@stomp/stompjs";
import { appConfig } from "@/config/env";
import { getValidAccessToken, subscribeAccessTokenChanged } from "@/core/auth/oidcSession";
import type { NotificationUserResponse } from "@/features/notifications/api/notificationApi";

type NotificationRealtimeOptions = {
  onConnected?: (reconnected: boolean) => void;
  onError?: (error: Error) => void;
  onNotification: (notification: NotificationUserResponse) => void;
};

const NOTIFICATION_DESTINATION = "/user/queue/notifications";

export function subscribeNotificationRealtime({ onConnected, onError, onNotification }: NotificationRealtimeOptions) {
  let active = true;
  let connectedOnce = false;
  const client = new Client({
    brokerURL: buildNotificationWebSocketUrl(),
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    reconnectDelay: 5000,
    beforeConnect: async () => {
      const accessToken = await getValidAccessToken();
      if (!active || !accessToken) {
        client.deactivate();
        return;
      }
      client.connectHeaders = {
        Authorization: `Bearer ${accessToken}`,
      };
    },
    onConnect: () => {
      client.subscribe(NOTIFICATION_DESTINATION, (message) => {
        try {
          onNotification(JSON.parse(message.body) as NotificationUserResponse);
        } catch {
          onError?.(new Error("Invalid notification realtime message"));
        }
      });
      onConnected?.(connectedOnce);
      connectedOnce = true;
    },
    onStompError: (frame) => {
      onError?.(new Error(frame.body || frame.headers.message || "Notification realtime error"));
      client.reconnectDelay = 0;
      void client.deactivate();
    },
    onWebSocketError: () => {
      onError?.(new Error("Notification realtime connection error"));
    },
  });

  const unsubscribeTokenChange = subscribeAccessTokenChanged(() => {
    if (!active) return;
    void client.deactivate().then(() => {
      if (!active) return;
      client.reconnectDelay = 5000;
      client.activate();
    });
  });
  client.activate();

  return () => {
    active = false;
    unsubscribeTokenChange();
    client.reconnectDelay = 0;
    void client.deactivate();
  };
}

function buildNotificationWebSocketUrl() {
  const apiUrl = new URL(appConfig.apiBaseUrl, window.location.origin);
  const protocol = apiUrl.protocol === "https:" ? "wss:" : "ws:";
  const basePath = apiUrl.pathname.replace(/\/api\/?$/, "");
  return `${protocol}//${apiUrl.host}${basePath}/ws`;
}
