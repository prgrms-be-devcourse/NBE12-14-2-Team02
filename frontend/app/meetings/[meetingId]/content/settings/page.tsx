"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import AppShell from "@/app/_components/AppShell";
import DeadlineInput from "@/app/_components/DeadlineInput";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, Message, PageTitle, formatDate } from "@/app/_components/ui";
import { apiFetch, jsonBody } from "@/app/_lib/api";
import { formatDeadlineInput, parseDeadline } from "@/app/_lib/deadline";
import type { ContentPoll } from "@/app/_lib/types";

export default function ContentSettingsPage() {
  const { meetingId } = useParams<{ meetingId: string }>();
  const [poll, setPoll] = useState<ContentPoll | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    apiFetch<ContentPoll>(`/api/meetings/${meetingId}/content-poll`).then(setPoll).catch((cause) => setError(cause instanceof Error ? cause.message : "투표 설정을 불러오지 못했습니다."));
  }, [meetingId]);

  async function updateDeadline(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setMessage("");
    try {
      const deadline = parseDeadline(new FormData(event.currentTarget).get("deadline"));
      const updated = await apiFetch<{ deadline: string }>(`/api/meetings/${meetingId}/content-poll`, {
        method: "PATCH",
        body: jsonBody({ deadline }),
      });
      setPoll((current) => current ? { ...current, deadline: updated.deadline } : current);
      setMessage("콘텐츠 투표 마감 시간을 변경했습니다.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "마감 시간을 변경하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  }

  return <AppShell>
    <div className="breadcrumb"><Link href={`/meetings/${meetingId}/content`}>← 콘텐츠 투표</Link><span>/</span><span>투표 설정</span></div>
    <PageTitle eyebrow="Poll settings" title="콘텐츠 투표 설정" description="호스트만 투표 마감 시간을 변경할 수 있습니다." />
    <MeetingTabs meetingId={meetingId} active="content" />
    {error && <Message tone="error">{error}</Message>}
    {message && <Message tone="success">{message}</Message>}
    {poll && <Card className="settings-card">
      <div className="row between"><div><Badge tone={poll.status === "OPEN" ? "green" : "gray"}>{poll.status === "OPEN" ? "투표 진행 중" : "투표 마감"}</Badge><h2>마감 시간</h2><p>현재 마감: <strong>{formatDate(poll.deadline)}</strong></p></div><span className="feature-icon" aria-hidden="true">◷</span></div>
      {poll.status === "OPEN" ? <form className="stack top-gap" onSubmit={updateDeadline}>
        <DeadlineInput key={poll.deadline} label="새 마감 시간" defaultValue={formatDeadlineInput(poll.deadline)} />
        <Message>마감 시간은 현재보다 이후의 정각으로만 변경할 수 있습니다.</Message>
        <div className="form-actions"><Link className="button button-ghost" href={`/meetings/${meetingId}/content`}>취소</Link><button className="button button-primary" disabled={saving}>{saving ? "변경 중…" : "마감 시간 변경"}</button></div>
      </form> : <Message>마감된 투표는 설정을 변경할 수 없습니다.</Message>}
    </Card>}
  </AppShell>;
}
