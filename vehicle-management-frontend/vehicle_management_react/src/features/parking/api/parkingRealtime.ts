import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import { appConfig } from "@/config/env";
import { getValidAccessToken } from "@/core/auth/tokenRefresh";

export type ParkingRealtimeEvent = {
  eventId: string;
  eventType:
    | "PARKING_SPACE_STATUS_CHANGED"
    | "PARKING_SPACE_ALLOCATION_CHANGED"
    | "PARKING_LAYOUT_PUBLISHED"
    | "PARKING_AVAILABILITY_CHANGED";
  occurredAt: string;
  parkingLotId: string;
  levelId: string | null;
  zoneId: string | null;
  parkingSpaceId: string | null;
  status: string | null;
  layoutVersion: number | null;
};

type ParkingRealtimeOptions = {
  parkingLotId: string | null;
  enabled?: boolean;
  onEvent: (event: ParkingRealtimeEvent) => void;
  onReconnect: () => void;
  onError?: (error: Error) => void;
};

export function useParkingRealtime({
  enabled = true,
  onError,
  onEvent,
  onReconnect,
  parkingLotId,
}: ParkingRealtimeOptions) {
  const handlers = useRef({ onEvent, onReconnect, onError });
  handlers.current = { onEvent, onReconnect, onError };

  useEffect(() => {
    if (!enabled || !parkingLotId) return;
    let active = true;
    let reconnected = false;
    const client = new Client({
      brokerURL: buildParkingWebSocketUrl(),
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
        if (reconnected) {
          handlers.current.onReconnect();
        }
        reconnected = true;
        client.subscribe(`/topic/parking-lots/${parkingLotId}`, (message) => {
          try {
            handlers.current.onEvent(JSON.parse(message.body) as ParkingRealtimeEvent);
          } catch {
            handlers.current.onError?.(new Error("Invalid parking realtime event"));
          }
        });
      },
      onStompError: (frame) => {
        handlers.current.onError?.(
          new Error(frame.body || frame.headers.message || "Parking realtime error"),
        );
      },
      onWebSocketError: () => {
        handlers.current.onError?.(new Error("Parking realtime connection error"));
      },
    });

    client.activate();
    return () => {
      active = false;
      client.reconnectDelay = 0;
      void client.deactivate();
    };
  }, [enabled, parkingLotId]);
}

function buildParkingWebSocketUrl() {
  const apiUrl = new URL(appConfig.apiBaseUrl, window.location.origin);
  const protocol = apiUrl.protocol === "https:" ? "wss:" : "ws:";
  const basePath = apiUrl.pathname.replace(/\/api\/?$/, "");
  return `${protocol}//${apiUrl.host}${basePath}/ws`;
}
