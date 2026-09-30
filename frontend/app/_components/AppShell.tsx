"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import {
  apiFetch,
  clearAccessToken,
  getAccessToken,
  hasValidAccessToken,
  NOTIFICATIONS_CHANGED,
  reissueAccessToken,
} from "@/app/_lib/api";
import { loginPathFromCurrentLocation } from "@/app/_lib/redirect";
import type { NotificationItem } from "@/app/_lib/types";

export default function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [authed, setAuthed] = useState(false);
  const [unread, setUnread] = useState(0);

  // 인증 확인: 토큰이 없거나 만료·손상됐으면 refresh 쿠키로 재발급을 시도하고, 그것도 실패하면 로그인 페이지로 보낸다
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const token = hasValidAccessToken()
        ? getAccessToken()
        : await reissueAccessToken().catch(() => null);
      if (cancelled) return;
      if (token) setAuthed(true);
      else router.replace(loginPathFromCurrentLocation());
    })();
    return () => { cancelled = true; };
  }, [router]);

  // 안 읽은 알림 개수 조회 (인증 확인 후, 페이지 이동마다 + 알림 변경 이벤트마다 갱신)
  useEffect(() => {
    if (!authed) return;
    function refresh() {
      apiFetch<NotificationItem[]>("/api/notifications")
        .then((items) => setUnread(items.filter((item) => !item.isRead).length))
        .catch(() => setUnread(0));
    }
    refresh();
    window.addEventListener(NOTIFICATIONS_CHANGED, refresh);
    return () => window.removeEventListener(NOTIFICATIONS_CHANGED, refresh);
  }, [authed, pathname]);

  // 로그아웃: 서버 요청 성공 여부와 관계없이 토큰을 지우고 로그인 페이지로 이동
  async function logout() {
    try {
      await apiFetch<void>("/api/auth/logout", { method: "POST" });
    } catch {
      /* 이미 인증이 없으면 로그아웃된 상태로 본다 */
    } finally {
      clearAccessToken();
      router.push("/login");
    }
  }

  // 인증 확인 전에는 자식 페이지를 렌더링하지 않아 화면 깜빡임과 불필요한 API 호출을 막는다
  if (!authed) return null;

  const nav = [
    { href: "/", label: "내 모임" },
    { href: "/notifications", label: `알림함${unread ? ` ${unread}` : ""}` },
    { href: "/mypage", label: "마이페이지" },
  ];

  return (
    <div className="app-frame">
      <header className="topbar">
        <Link href="/" className="brand">
          <span>M</span>MOIM
        </Link>

        <nav>
          {nav.map((item) => (
            <Link key={item.href} href={item.href} className={pathname === item.href ? "active" : ""}>
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="topbar-actions">
          <Link className="button button-primary button-small" href="/meetings/new">
            + 모임 만들기
          </Link>
          <button className="button button-ghost button-small" onClick={logout}>
            로그아웃
          </button>
        </div>
      </header>

      <main className="page-container">{children}</main>
    </div>
  );
}
