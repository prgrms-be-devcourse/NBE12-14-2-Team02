"use client";
import { useEffect, useState } from "react";
import AppShell from "@/app/_components/AppShell";
import { Badge, Card, EmptyState, Message, PageTitle, formatDate } from "@/app/_components/ui";
import { apiFetch } from "@/app/_lib/api";
import type { NotificationItem } from "@/app/_lib/types";

export default function NotificationsPage() {
  const [items, setItems] = useState<NotificationItem[]>([]); const [error, setError] = useState("");
  useEffect(() => { apiFetch<NotificationItem[]>("/api/notifications").then(setItems).catch((e) => setError(e.message)); }, []);
  async function markRead(id: number) { try { const updated = await apiFetch<NotificationItem>(`/api/notifications/${id}/read`, { method: "PATCH" }); setItems((all) => all.map((item) => item.id === id ? updated : item)); } catch (e) { setError(e instanceof Error ? e.message : "읽음 처리하지 못했습니다."); } }
  return <AppShell><PageTitle eyebrow="Notifications" title="알림함" description={`읽지 않은 알림 ${items.filter((item) => !item.isRead).length}개`} />{error && <Message tone="error">{error}</Message>}<div className="stack">{items.map((item) => <Card key={item.id} className={`notification ${item.isRead ? "" : "unread"}`}><div className="row between"><Badge tone={item.isRead ? "gray" : "purple"}>{item.type}</Badge><span className="muted">{formatDate(item.createdAt)}</span></div><h3>{item.title}</h3><p>{item.content}</p>{!item.isRead && <button className="button button-secondary button-small" onClick={() => markRead(item.id)}>읽음 처리</button>}</Card>)}</div>{items.length === 0 && <Card><EmptyState title="알림이 없어요" description="새로운 소식이 생기면 이곳에 표시됩니다." /></Card>}</AppShell>;
}
