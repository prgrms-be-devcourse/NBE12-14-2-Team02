"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import AppShell from "@/app/_components/AppShell";
import { Badge, Card, EmptyState, Message, PageTitle, formatDate } from "@/app/_components/ui";
import { apiFetch, notifyNotificationsChanged } from "@/app/_lib/api";
import type { NotificationItem } from "@/app/_lib/types";

function resultPath(item: NotificationItem) {
  if (item.meetingId == null || !item.redirectUrl) return null;
  const url = item.redirectUrl;
  if (url.includes("content-poll") || url.includes("/content")) return `/meetings/${item.meetingId}/content/results`;
  if (url.includes("schedule-poll") || url.includes("/schedule")) return `/meetings/${item.meetingId}/schedule/results`;
  if (url.includes("settlement")) return `/meetings/${item.meetingId}/settlement`;
  return null;
}

export default function NotificationsPage() {
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    apiFetch<NotificationItem[]>("/api/notifications").then(setItems).catch((e) => setError(e.message));
  }, []);

  async function markRead(id: number) {
    try {
      const updated = await apiFetch<NotificationItem>(`/api/notifications/${id}/read`, { method: "PATCH" });
      setItems((all) => all.map((item) => item.id === id ? updated : item));
      notifyNotificationsChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : "읽음 처리하지 못했습니다.");
    }
  }

  return (
    <AppShell>
      <PageTitle eyebrow="Notifications" title="알림함" description={`읽지 않은 알림 ${items.filter((item) => !item.isRead).length}개`} />
      {error && <Message tone="error">{error}</Message>}
      <div className="stack">
        {items.map((item) => {
          const href = resultPath(item);
          return (
            <Card key={item.id} className={`notification ${item.isRead ? "" : "unread"}`}>
              <div className="row between">
                <Badge tone={item.isRead ? "gray" : "purple"}>{item.type}</Badge>
                <span className="muted">{formatDate(item.createdAt)}</span>
              </div>
              <h3>{item.title}</h3>
              <p>{item.content}</p>
              <div className="row">
                {href && <Link className="button button-secondary button-small" href={href} onClick={() => { if (!item.isRead) void markRead(item.id); }}>결과 보기</Link>}
                {!item.isRead && <button className="button button-ghost button-small" onClick={() => markRead(item.id)}>읽음 처리</button>}
              </div>
            </Card>
          );
        })}
      </div>
      {items.length === 0 && <Card><EmptyState title="알림이 없어요" description="새로운 소식이 생기면 이곳에 표시됩니다." /></Card>}
    </AppShell>
  );
}
