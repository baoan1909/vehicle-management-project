import { Client } from "@stomp/stompjs";
import { appConfig } from "@/config/env";
import { getValidAccessToken } from "@/core/auth/tokenRefresh";
import type { ChatRealtimeEvent } from "@/features/support/api/chatApi";

type ChatRealtimeOptions = {
  conversationId?: string | null;
  onEvent: (event: ChatRealtimeEvent) => void;
  onError?: (error: Error) => void;
};

const CHAT_INBOX_DESTINATION = "/user/topic/chat";

export function subscribeChatRealtime({ conversationId, onError, onEvent }: ChatRealtimeOptions) {
  let active = true;
  const destinations = buildChatRealtimeDestinations(conversationId);
  const client = new Client({
    brokerURL: buildChatWebSocketUrl(),
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
      destinations.forEach((destination) => {
        client.subscribe(destination, (message) => {
          try {
            onEvent(JSON.parse(message.body) as ChatRealtimeEvent);
          } catch {
            onError?.(new Error("Invalid chat realtime event"));
          }
        });
      });
    },
    onStompError: (frame) => {
      onError?.(new Error(frame.body || frame.headers.message || "Chat realtime error"));
      client.reconnectDelay = 0;
      void client.deactivate();
    },
    onWebSocketError: () => {
      onError?.(new Error("Chat realtime connection error"));
    },
  });

  client.activate();

  return () => {
    active = false;
    client.reconnectDelay = 0;
    void client.deactivate();
  };
}

function buildChatWebSocketUrl() {
  const apiUrl = new URL(appConfig.apiBaseUrl, window.location.origin);
  const protocol = apiUrl.protocol === "https:" ? "wss:" : "ws:";
  const basePath = apiUrl.pathname.replace(/\/api\/?$/, "");
  return `${protocol}//${apiUrl.host}${basePath}/ws`;
}

function buildChatRealtimeDestinations(conversationId: string | null | undefined) {
  const destinations = [CHAT_INBOX_DESTINATION];
  if (conversationId) {
    destinations.push(`/user/queue/chat/conversations/${conversationId}`);
  }

  return destinations;
}
