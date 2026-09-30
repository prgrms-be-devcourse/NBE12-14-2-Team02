"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, EmptyState, Message, PageTitle, formatDate } from "@/app/_components/ui";
import { ApiError, apiFetch, jsonBody } from "@/app/_lib/api";
import type { ScheduleCandidate, SchedulePoll } from "@/app/_lib/types";

export default function ScheduleManagePage() {
  const { meetingId } = useParams<{ meetingId: string }>();
  const [poll, setPoll] = useState<SchedulePoll | null>(null);
  const [missing, setMissing] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [editing, setEditing] = useState<number | null>(null);
  const [editDate, setEditDate] = useState("");

  const load = useCallback(() => apiFetch<SchedulePoll>(`/api/meetings/${meetingId}/schedule-poll`)
    .then((data) => { setPoll(data); setMissing(false); })
    .catch((cause) => {
      if (cause instanceof ApiError && cause.status === 404) setMissing(true);
      else setError(cause instanceof Error ? cause.message : "일정 투표를 불러오지 못했습니다.");
    }), [meetingId]);

  useEffect(() => { void load(); }, [load]);

  async function addCandidate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const candidateDate = new FormData(form).get("candidateDate");
    setError("");
    setMessage("");
    try {
      const candidate = await apiFetch<ScheduleCandidate>(`/api/meetings/${meetingId}/schedule-poll/candidates`, {
        method: "POST",
        body: jsonBody({ candidateDate }),
      });
      setPoll((current) => current ? { ...current, candidates: [...current.candidates, candidate] } : current);
      form.reset();
      setMessage("새로운 날짜 후보를 추가했습니다.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "후보를 추가하지 못했습니다.");
    }
  }

  async function updateCandidate(id: number) {
    setError("");
    setMessage("");
    try {
      const candidate = await apiFetch<ScheduleCandidate>(`/api/meetings/${meetingId}/schedule-poll/candidates/${id}`, {
        method: "PATCH",
        body: jsonBody({ candidateDate: editDate }),
      });
      setPoll((current) => current ? { ...current, candidates: current.candidates.map((item) => item.id === id ? candidate : item) } : current);
      setEditing(null);
      setMessage("날짜 후보를 수정했습니다.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "수정하지 못했습니다. 이미 응답이 있는 후보일 수 있습니다.");
    }
  }

  async function removeCandidate(id: number) {
    if (!window.confirm("이 날짜 후보를 삭제할까요?")) return;
    setError("");
    setMessage("");
    try {
      await apiFetch<void>(`/api/meetings/${meetingId}/schedule-poll/candidates/${id}`, { method: "DELETE" });
      setPoll((current) => current ? { ...current, candidates: current.candidates.filter((item) => item.id !== id) } : current);
      setMessage("날짜 후보를 삭제했습니다.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "삭제하지 못했습니다. 이미 응답이 있는 후보일 수 있습니다.");
    }
  }

  return <AppShell>
    <PageTitle
      eyebrow="Host tools"
      title="일정 후보 관리"
      description="모임원들이 투표할 날짜 후보를 최대 10개까지 관리하세요."
      action={<Link className="button button-secondary" href={`/meetings/${meetingId}/schedule/vote`}>투표 화면 보기</Link>}
    />
    <MeetingTabs meetingId={meetingId} active="schedule" />
    {message && <Message tone="success">{message}</Message>}
    {error && <Message tone="error">{error}</Message>}

    {missing && <Card><EmptyState title="일정 투표가 아직 없습니다" description="먼저 마감 시간을 정해 일정 투표를 만들어주세요." /><div className="form-actions"><Link className="button button-primary" href={`/meetings/${meetingId}/schedule`}>투표 만들기로 이동</Link></div></Card>}

    {poll && <div className="stack">
      <div className="poll-status-bar">
        <div><Badge tone={poll.status === "OPEN" ? "green" : "gray"}>{poll.status === "OPEN" ? "투표 진행 중" : "투표 마감"}</Badge><strong>후보 {poll.candidates.length}/10</strong></div>
        <span>◷ 마감 {formatDate(poll.deadline)}</span>
      </div>

      <Card className="candidate-manage-card">
        <div className="section-header"><div><h2>새 날짜 후보</h2><p>오늘 이후의 날짜를 선택해 주세요.</p></div></div>
        <form className="candidate-create-row" onSubmit={addCandidate}>
          <input className="input" name="candidateDate" type="date" required />
          <button className="button button-primary" disabled={poll.status === "CLOSED" || poll.candidates.length >= 10}>＋ 후보 추가</button>
        </form>

        <div className="divider" />
        <div className="candidate-list">
          {poll.candidates.map((candidate, index) => <div className="candidate-list-item" key={candidate.id}>
            <span className="candidate-number">{index + 1}</span>
            {editing === candidate.id ? (
              <input className="input" type="date" value={editDate} onChange={(event) => setEditDate(event.target.value)} />
            ) : (
              <div className="candidate-date"><strong>{formatDate(candidate.candidateDate)}</strong><small>일정 후보</small></div>
            )}
            <div className="row candidate-actions">
              {editing === candidate.id ? <>
                <button type="button" className="button button-primary button-small" onClick={() => updateCandidate(candidate.id)}>저장</button>
                <button type="button" className="button button-ghost button-small" onClick={() => setEditing(null)}>취소</button>
              </> : <button type="button" className="button button-ghost button-small" disabled={poll.status === "CLOSED"} onClick={() => { setEditing(candidate.id); setEditDate(candidate.candidateDate); }}>수정</button>}
              <button type="button" className="button button-danger button-small" disabled={poll.status === "CLOSED"} onClick={() => removeCandidate(candidate.id)}>삭제</button>
            </div>
          </div>)}
          {poll.candidates.length === 0 && <EmptyState title="등록된 날짜 후보가 없습니다" description="첫 번째 날짜 후보를 추가해 주세요." />}
        </div>
        <Message>이미 모임원의 응답이 있는 후보는 서버 정책에 따라 수정하거나 삭제할 수 없습니다.</Message>
      </Card>
    </div>}
  </AppShell>;
}
