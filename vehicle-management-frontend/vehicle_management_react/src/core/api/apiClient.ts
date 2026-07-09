import { appConfig } from "@/config/env";
import { getAccessToken } from "@/core/auth/session";

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

export async function apiClient<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const accessToken = getAccessToken();
  const response = await fetch(`${appConfig.apiBaseUrl}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...options.headers,
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  const contentType = response.headers.get("content-type") ?? "";
  const responseBody = contentType.includes("application/json") ? await response.json() : null;

  if (!response.ok) {
    const message =
      responseBody &&
      typeof responseBody === "object" &&
      "message" in responseBody &&
      typeof responseBody.message === "string"
        ? responseBody.message
        : `API error ${response.status}`;

    throw new Error(message);
  }

  if (responseBody === null) {
    throw new Error(`API response is not JSON for ${path}`);
  }

  return responseBody as T;
}
