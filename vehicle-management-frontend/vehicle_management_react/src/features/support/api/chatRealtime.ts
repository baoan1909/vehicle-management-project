import { appConfig } from "@/config/env";
import { getValidAccessToken } from "@/core/auth/tokenRefresh";
import type { ChatRealtimeEvent } from "@/features/support/api/chatApi";

type ChatRealtimeOptions = {
  conversationId?: string | null;
  onEvent: (event: ChatRealtimeEvent) => void;
  onError?: (error: Error) => void;
};

type StompFrame = {
  body: string;
  command: string;
  headers: Record<string, string>;
};

const STOMP_NULL = "\0";
const CHAT_INBOX_DESTINATION = "/user/topic/chat";

export function subscribeChatRealtime({ conversationId, onError, onEvent }: ChatRealtimeOptions) {
  let active = true;
  let socket: WebSocket | null = null;
  let reconnectTimerId: number | undefined;
  const destinations = buildChatRealtimeDestinations(conversationId);

  async function connect() {
    try {
      const accessToken = await getValidAccessToken();
      if (!active || !accessToken) return;

      socket = new WebSocket(buildChatWebSocketUrl());
      socket.onopen = () => {
        sendFrame(socket, "CONNECT", {
          "accept-version": "1.2",
          Authorization: `Bearer ${accessToken}`,
          "heart-beat": "0,0",
          host: window.location.host,
        });
      };
      socket.onmessage = (message) => handleSocketMessage(socket, String(message.data), destinations, onEvent, onError);
      socket.onerror = () => onError?.(new Error("Chat realtime connection error"));
      socket.onclose = () => scheduleReconnect();
    } catch (error) {
      onError?.(error instanceof Error ? error : new Error("Cannot connect chat realtime"));
      scheduleReconnect();
    }
  }

  function scheduleReconnect() {
    if (!active || reconnectTimerId !== undefined) return;
    reconnectTimerId = window.setTimeout(() => {
      reconnectTimerId = undefined;
      void connect();
    }, 5000);
  }

  void connect();

  return () => {
    active = false;
    if (reconnectTimerId !== undefined) {
      window.clearTimeout(reconnectTimerId);
    }
    if (socket?.readyState === WebSocket.OPEN) {
      sendFrame(socket, "DISCONNECT", {});
    }
    socket?.close();
    socket = null;
  };
}

function handleSocketMessage(
  socket: WebSocket | null,
  rawMessage: string,
  destinations: string[],
  onEvent: (event: ChatRealtimeEvent) => void,
  onError?: (error: Error) => void,
) {
  parseFrames(rawMessage).forEach((frame) => {
    if (frame.command === "CONNECTED") {
      destinations.forEach((destination, index) => {
        sendFrame(socket, "SUBSCRIBE", {
          ack: "auto",
          destination,
          id: `chat-realtime-${index}`,
        });
      });
      return;
    }

    if (frame.command === "MESSAGE") {
      try {
        onEvent(JSON.parse(frame.body) as ChatRealtimeEvent);
      } catch {
        onError?.(new Error("Invalid chat realtime event"));
      }
      return;
    }

    if (frame.command === "ERROR") {
      onError?.(new Error(frame.body || frame.headers.message || "Chat realtime error"));
    }
  });
}

function parseFrames(rawMessage: string): StompFrame[] {
  return rawMessage
    .split(STOMP_NULL)
    .map((chunk) => chunk.replace(/^\s+/, ""))
    .filter((chunk) => chunk.trim().length > 0)
    .map(parseFrame);
}

function parseFrame(rawFrame: string): StompFrame {
  const normalizedFrame = rawFrame.replace(/\r\n/g, "\n");
  const separatorIndex = normalizedFrame.indexOf("\n\n");
  const headerBlock = separatorIndex >= 0 ? normalizedFrame.slice(0, separatorIndex) : normalizedFrame;
  const body = separatorIndex >= 0 ? normalizedFrame.slice(separatorIndex + 2) : "";
  const [command, ...headerLines] = headerBlock.split("\n");
  const headers = headerLines.reduce<Record<string, string>>((result, line) => {
    const separatorIndex = line.indexOf(":");
    if (separatorIndex <= 0) return result;
    result[line.slice(0, separatorIndex)] = line.slice(separatorIndex + 1);
    return result;
  }, {});

  return { body, command: command.trim(), headers };
}

function sendFrame(socket: WebSocket | null, command: string, headers: Record<string, string>) {
  if (!socket || socket.readyState !== WebSocket.OPEN) return;
  const serializedHeaders = Object.entries(headers)
    .map(([key, value]) => `${key}:${value}`)
    .join("\n");
  socket.send(`${command}\n${serializedHeaders}\n\n${STOMP_NULL}`);
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
