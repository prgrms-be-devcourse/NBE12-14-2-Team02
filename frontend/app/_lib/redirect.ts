"use client";

export const REDIRECT_PARAM = "redirect";

// 로그인 후 돌아갈 경로는 앱 내부 경로만 허용한다.
// "//evil.com" 이나 "/\evil.com" 은 브라우저가 외부 주소로 해석하므로 함께 막는다.
export function safeRedirect(value: string | null | undefined) {
  if (!value || !value.startsWith("/")) return null;
  if (value.startsWith("//") || value.startsWith("/\\")) return null;
  return value;
}

// redirect 파라미터를 붙인 경로. 값이 없으면 원래 경로를 그대로 쓴다.
export function withRedirect(path: string, redirect: string | null) {
  if (!redirect) return path;
  return `${path}?${REDIRECT_PARAM}=${encodeURIComponent(redirect)}`;
}

// 인증이 없어 로그인 페이지로 보낼 때 쓰는 경로.
// 현재 위치(쿼리 포함)를 redirect로 실어 보내 로그인 후 원래 화면으로 돌아오게 한다.
export function loginPathFromCurrentLocation() {
  if (typeof window === "undefined") return "/login";

  const current = window.location.pathname + window.location.search;

  // 인증 페이지나 홈은 되돌아갈 목적지로 의미가 없다 (로그인 후 기본 이동지가 홈이다)
  if (current === "/" || current.startsWith("/login") || current.startsWith("/signup")) {
    return "/login";
  }

  return withRedirect("/login", current);
}
