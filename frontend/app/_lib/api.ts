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

// JWT payload 읽기 (서명 검증은 서버가 하고, 여기서는 화면 처리용으로만 쓴다)
function readTokenPayload(token: string): { sub?: string; exp?: number } | null {
  try {
    // JWT는 base64url 인코딩이라 atob가 읽을 수 있는 base64로 바꿔준다
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

// 저장된 access token이 읽을 수 있는 형식이고 만료되지 않았는지 확인
export function hasValidAccessToken() {
  const token = getAccessToken();
  if (!token) return false;
  const payload = readTokenPayload(token);
  return typeof payload?.exp === "number" && payload.exp * 1000 > Date.now();
}

export function getCurrentUserId() {
  const token = getAccessToken();
  if (!token) return null;
  const payload = readTokenPayload(token);
  return payload ? Number(payload.sub) : null;
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

export async function reissueAccessToken() {
  const response = await fetch("/api/auth/reissue", { method: "POST", credentials: "include" });
  const envelope = await parseEnvelope<{ accessToken: string }>(response);
  if (!response.ok || !envelope?.data?.accessToken) return null;
  setAccessToken(envelope.data.accessToken);
  return envelope.data.accessToken;
}

export async function apiFetch<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers);
  const token = getAccessToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, { ...init, headers, credentials: "include" });

  // access token 만료 시 한 번만 재발급 후 재요청
  if (response.status === 401 && retry && path !== "/api/auth/reissue") {
    const renewed = await reissueAccessToken();
    if (renewed) {
      return apiFetch<T>(path, init, false);
    }
  }

  // 재발급까지 실패하면 로그인 페이지로 이동
  // /api/auth/* 는 제외: 로그인 실패(비밀번호 오류 등)도 401이라 에러 메시지를 보여줘야 한다
  if (response.status === 401 && !path.startsWith("/api/auth/")) {
    clearAccessToken();
    // 컴포넌트 밖이라 useRouter를 쓸 수 없어 전체 페이지 이동으로 처리한다
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.href = "/login";
  }

  const envelope = await parseEnvelope<T>(response);
  if (!response.ok) {
    throw new ApiError(
      envelope?.message || envelope?.error?.message || "요청을 처리하지 못했습니다.",
      response.status,
    );
  }
  return envelope?.data as T;
}

export const jsonBody = (value: unknown) => JSON.stringify(value);

export async function apiFetchBlob(path: string, retry = true): Promise<Blob> {
  const token = getAccessToken();
  const response = await fetch(path, { headers: token ? { Authorization: `Bearer ${token}` } : {}, credentials: "include" });
  if (response.status === 401 && retry && await reissueAccessToken()) return apiFetchBlob(path, false);
  if (!response.ok) {
    const envelope = await parseEnvelope<never>(response);
    throw new ApiError(envelope?.message || "영수증을 불러오지 못했습니다.", response.status);
  }
  return response.blob();
}

export const NOTIFICATIONS_CHANGED = "moim-notifications-changed";

export function notifyNotificationsChanged() {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED));
}
