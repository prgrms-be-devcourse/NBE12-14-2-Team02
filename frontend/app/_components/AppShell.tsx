"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import { apiFetch, clearAccessToken } from "@/app/_lib/api";
import type { NotificationItem } from "@/app/_lib/types";

export default function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [unread, setUnread] = useState(0);
  useEffect(() => { apiFetch<NotificationItem[]>("/api/notifications").then((items) => setUnread(items.filter((item) => !item.isRead).length)).catch(() => setUnread(0)); }, [pathname]);
  async function logout() { try { await apiFetch<void>("/api/auth/logout", { method: "POST" }); } finally { clearAccessToken(); router.push("/login"); } }
  const nav = [{ href: "/", label: "내 모임" }, { href: "/notifications", label: `알림함${unread ? ` ${unread}` : ""}` }, { href: "/mypage", label: "마이페이지" }];
  return <div className="app-frame"><header className="topbar"><Link href="/" className="brand"><span>M</span>MOIM</Link><nav>{nav.map((item) => <Link key={item.href} href={item.href} className={pathname === item.href ? "active" : ""}>{item.label}</Link>)}</nav><div className="topbar-actions"><Link className="button button-primary button-small" href="/meetings/new">+ 모임 만들기</Link><button className="button button-ghost button-small" onClick={logout}>로그아웃</button></div></header><main className="page-container">{children}</main></div>;
}
