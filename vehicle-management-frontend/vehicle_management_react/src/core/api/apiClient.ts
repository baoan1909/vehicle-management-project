import { appConfig } from "@/config/env";
import { getValidAccessToken, refreshAccessToken } from "@/core/auth/tokenRefresh";

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

export async function apiClient<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const accessToken = await getValidAccessToken();
  let response = await sendJsonRequest(path, options, accessToken);

  if (response.status === 401 && accessToken) {
    const refreshedToken = await refreshAccessToken();
    if (refreshedToken) {
      response = await sendJsonRequest(path, options, refreshedToken);
    }
  }

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

function sendJsonRequest(path: string, options: RequestOptions, accessToken: string | null) {
  return fetch(`${appConfig.apiBaseUrl}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...options.headers,
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });
}
