"use client";

export type ApiEnvelope<T> = {
  success: boolean;
  code: number;
  message: string;
  data: T;
  error?: { code?: string; message?: string };
};

export class ApiError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message);
  }
}

const ACCESS_TOKEN_KEY = "moim.accessToken";

export function getAccessToken() {
  if (typeof window === "undefined") return null;
  return window.sessionStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setAccessToken(token: string) {
  window.sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function clearAccessToken() {
  window.sessionStorage.removeItem(ACCESS_TOKEN_KEY);
}

export function getCurrentUserId() {
  const token = getAccessToken();
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return Number(payload.sub);
  } catch {
    return null;
  }
}

async function parseEnvelope<T>(response: Response): Promise<ApiEnvelope<T> | null> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text) as ApiEnvelope<T>;
  } catch {
    return null;
  }
}

async function reissueAccessToken() {
  const response = await fetch("/api/auth/reissue", { method: "POST", credentials: "include" });
  const envelope = await parseEnvelope<{ accessToken: string }>(response);
  if (!response.ok || !envelope?.data?.accessToken) return null;
  setAccessToken(envelope.data.accessToken);
  return envelope.data.accessToken;
}

export async function apiFetch<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers);
  const token = getAccessToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");

  const response = await fetch(path, { ...init, headers, credentials: "include" });
  if (response.status === 401 && retry && path !== "/api/auth/reissue") {
    const renewed = await reissueAccessToken();
    if (renewed) return apiFetch<T>(path, init, false);
  }

  const envelope = await parseEnvelope<T>(response);
  if (!response.ok) {
    throw new ApiError(envelope?.message || envelope?.error?.message || "요청을 처리하지 못했습니다.", response.status);
  }
  return envelope?.data as T;
}

export const jsonBody = (value: unknown) => JSON.stringify(value);
