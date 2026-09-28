"use client";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, Field, Message, PageTitle, formatDate } from "@/app/_components/ui";
import { ApiError, apiFetch, getCurrentUserId, jsonBody } from "@/app/_lib/api";
import type { ContentPoll, Meeting, MeetingMember } from "@/app/_lib/types";

type Preference = "PREFER" | "AVAILABLE" | "DISLIKE";
const choices: Array<{ value: Preference; label: string }> = [
  { value: "PREFER", label: "선호 3점" },
  { value: "AVAILABLE", label: "가능 2점" },
  { value: "DISLIKE", label: "별로 1점" },
];

export default function ContentPollPage() {
  const { meetingId } = useParams<{ meetingId: string }>();
  const [poll, setPoll] = useState<ContentPoll | null>(null);
  const [members, setMembers] = useState<MeetingMember[]>([]);
  const [host, setHost] = useState<boolean | null>(null);
  const [missing, setMissing] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const myMemberId = useMemo(() => members.find((member) => member.userId === getCurrentUserId())?.id, [members]);
  const closed = poll?.status === "CLOSED";

  const load = useCallback(() => apiFetch<ContentPoll>(`/api/meetings/${meetingId}/content-poll`).then((data) => {
    setPoll(data);
    setMissing(false);
  }).catch((e) => {
    if (e instanceof ApiError && e.status === 404) setMissing(true);
    else setError(e.message);
  }), [meetingId]);

  useEffect(() => {
    load();
    apiFetch<Meeting>(`/api/meetings/${meetingId}`).then((meeting) => setHost(meeting.hostId === getCurrentUserId())).catch(() => setHost(false));
    apiFetch<MeetingMember[]>(`/api/meetings/${meetingId}/members`).then(setMembers).catch(() => undefined);
  }, [meetingId, load]);

  async function createPoll(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
      await apiFetch(`/api/meetings/${meetingId}/content-poll`, { method: "POST", body: jsonBody({ deadline: form.get("deadline") }) });
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "투표를 만들지 못했습니다.");
    }
  }

  async function updateDeadline(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
      const data = await apiFetch<{ deadline: string }>(`/api/meetings/${meetingId}/content-poll`, { method: "PATCH", body: jsonBody({ deadline: form.get("deadline") }) });
      setPoll((current) => current ? { ...current, deadline: data.deadline } : current);
      setMessage("마감 시간을 변경했습니다.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "마감 시간을 바꾸지 못했습니다.");
    }
  }

  async function addCandidate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    try {
      await apiFetch(`/api/meetings/${meetingId}/content-poll/candidates`, { method: "POST", body: jsonBody({ title: data.get("title"), description: data.get("description") }) });
      form.reset();
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "후보를 등록하지 못했습니다.");
    }
  }

  async function vote(candidateId: number, preference: Preference) {
    try {
      await apiFetch(`/api/meetings/${meetingId}/content-votes`, { method: "PUT", body: jsonBody({ candidateId, preference }) });
      setPoll((current) => current ? { ...current, candidates: current.candidates.map((candidate) => candidate.candidateId === candidateId ? { ...candidate, myPreference: preference } : candidate) } : current);
      setMessage("선호도를 저장했습니다.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장하지 못했습니다.");
    }
  }

  function startEdit(id: number, title: string, description: string | null) {
    setEditingId(id);
    setEditTitle(title);
    setEditDescription(description || "");
    setError("");
    setMessage("");
  }

  function cancelEdit() {
    setEditingId(null);
  }

  async function saveEdit(id: number) {
    const title = editTitle.trim();
    if (!title) {
      setError("제목을 입력해 주세요.");
      return;
    }
    try {
      await apiFetch(`/api/meetings/${meetingId}/content-poll/candidates/${id}`, { method: "PATCH", body: jsonBody({ title, description: editDescription.trim() }) });
      setEditingId(null);
      setMessage("후보를 수정했습니다.");
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "수정하지 못했습니다. 이미 투표가 있는 후보일 수 있습니다.");
    }
  }

  async function removeCandidate(id: number) {
    try {
      await apiFetch<void>(`/api/meetings/${meetingId}/content-poll/candidates/${id}`, { method: "DELETE" });
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "삭제하지 못했습니다. 이미 투표가 있는 후보일 수 있습니다.");
    }
  }

  return (
    <AppShell>
      <PageTitle eyebrow="Content poll" title="콘텐츠 후보 및 투표" description="모임에서 함께할 활동을 제안하고 후보별 선호도를 저장하세요." action={<Link className="button button-secondary" href={`/meetings/${meetingId}/content/results`}>결과 보기</Link>} />
      <MeetingTabs meetingId={meetingId} active="content" />
      {message && <Message tone="success">{message}</Message>}
      {error && <Message tone="error">{error}</Message>}
      {missing && host && (
        <Card>
          <h2>콘텐츠 투표 만들기</h2>
          <form className="row" onSubmit={createPoll}>
            <input className="input" type="datetime-local" name="deadline" required />
            <button className="button button-primary">투표 만들기</button>
          </form>
        </Card>
      )}
      {missing && host === false && <Card><h2>아직 콘텐츠 투표가 없어요</h2><p>호스트가 투표를 만들면 후보를 등록할 수 있습니다.</p></Card>}
      {poll && (
        <div className="stack">
          <div className="row between">
            <Badge tone={poll.status === "OPEN" ? "green" : "gray"}>{poll.status}</Badge>
            <span className="muted">마감 {formatDate(poll.deadline)}</span>
          </div>
          {host && !closed && (
            <Card>
              <h2>마감 시간 변경</h2>
              <form className="row" onSubmit={updateDeadline}>
                <Field label="새 마감"><input className="input" name="deadline" type="datetime-local" required /></Field>
                <button className="button button-secondary">변경</button>
              </form>
            </Card>
          )}
          {!closed && (
            <Card>
              <h2>새 콘텐츠 후보</h2>
              <form className="grid grid-2" onSubmit={addCandidate}>
                <Field label="제목"><input className="input" name="title" maxLength={100} required /></Field>
                <Field label="설명 (선택)"><input className="input" name="description" maxLength={500} /></Field>
                <button className="button button-primary">후보 등록</button>
              </form>
            </Card>
          )}
          {poll.candidates.map((candidate) => {
            const editing = editingId === candidate.candidateId;
            return (
            <Card key={candidate.candidateId}>
              <div className="section-header">
                {editing ? (
                  <div className="grid grid-2" style={{ flex: 1 }}>
                    <Field label="제목"><input className="input" value={editTitle} maxLength={100} onChange={(event) => setEditTitle(event.target.value)} required /></Field>
                    <Field label="설명 (선택)"><input className="input" value={editDescription} maxLength={500} onChange={(event) => setEditDescription(event.target.value)} /></Field>
                  </div>
                ) : (
                  <div>
                    <h2>{candidate.title}</h2>
                    <p>{candidate.description || "설명이 없습니다."}</p>
                  </div>
                )}
                {!closed && candidate.createdByMemberId === myMemberId && (
                  <div className="row">
                    {editing ? (
                      <>
                        <button className="button button-primary button-small" onClick={() => saveEdit(candidate.candidateId)}>저장</button>
                        <button className="button button-ghost button-small" onClick={cancelEdit}>취소</button>
                      </>
                    ) : (
                      <>
                        <button className="button button-ghost button-small" onClick={() => startEdit(candidate.candidateId, candidate.title, candidate.description)}>수정</button>
                        <button className="button button-danger button-small" onClick={() => removeCandidate(candidate.candidateId)}>삭제</button>
                      </>
                    )}
                  </div>
                )}
              </div>
              <div className="pill-options">
                {choices.map((choice) => (
                  <button className={`choice ${candidate.myPreference === choice.value ? "selected" : ""}`} key={choice.value} disabled={closed} onClick={() => vote(candidate.candidateId, choice.value)}>{choice.label}</button>
                ))}
              </div>
            </Card>
            );
          })}
        </div>
      )}
    </AppShell>
  );
}
