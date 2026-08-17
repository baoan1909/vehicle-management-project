import { appConfig } from "@/config/env";
import { localizeApiMessage, localizeApiResponseBody } from "@/core/api/apiMessage";
import { getValidAccessToken, refreshAccessToken } from "@/core/auth/tokenRefresh";

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  skipAuth?: boolean;
};

export async function apiClient<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { skipAuth, ...requestOptions } = options;
  const accessToken = skipAuth ? null : await getValidAccessToken();
  let response = await sendJsonRequest(path, requestOptions, accessToken);

  if (!skipAuth && response.status === 401 && accessToken) {
    const refreshedToken = await refreshAccessToken();
    if (refreshedToken) {
      response = await sendJsonRequest(path, requestOptions, refreshedToken);
    }
  }

  const contentType = response.headers.get("content-type") ?? "";
  const responseBody = contentType.includes("application/json") ? await response.json() : null;

  if (!response.ok) {
    const rawMessage =
      responseBody &&
      typeof responseBody === "object" &&
      "message" in responseBody &&
      typeof responseBody.message === "string"
        ? responseBody.message
        : null;

    throw new Error(localizeApiMessage(rawMessage, response.status));
  }

  if (responseBody === null) {
    throw new Error("Máy chủ trả về dữ liệu không đúng định dạng.");
  }

  return localizeApiResponseBody(responseBody, response.status) as T;
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
