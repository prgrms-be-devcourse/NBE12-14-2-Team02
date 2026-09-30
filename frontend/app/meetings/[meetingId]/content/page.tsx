"use client";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import AppShell from "@/app/_components/AppShell";
import DeadlineInput from "@/app/_components/DeadlineInput";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, Field, Message, PageTitle, formatDate } from "@/app/_components/ui";
import { ApiError, apiFetch, getCurrentUserId, jsonBody } from "@/app/_lib/api";
import { parseDeadline } from "@/app/_lib/deadline";
import type { ContentPoll, Meeting, MeetingMember } from "@/app/_lib/types";

type Preference = "PREFER" | "AVAILABLE" | "DISLIKE";
const choices: Array<{ value: Preference; label: string }> = [
  { value: "PREFER", label: "★ 최우선 선호" },
  { value: "AVAILABLE", label: "✓ 참여 가능" },
  { value: "DISLIKE", label: "− 비선호" },
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
      await apiFetch(`/api/meetings/${meetingId}/content-poll`, { method: "POST", body: jsonBody({ deadline: parseDeadline(form.get("deadline")) }) });
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "투표를 만들지 못했습니다.");
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
      <PageTitle eyebrow="Content poll" title="콘텐츠 투표" description="함께할 활동과 장소를 제안하고 후보별 선호도를 남겨주세요." action={poll ? <div className="row">{host && <Link className="button button-ghost" href={`/meetings/${meetingId}/content/settings`}>⚙ 투표 설정</Link>}{!closed && <Link className="button button-ghost" href={`/meetings/${meetingId}/content/new`}>＋ 후보 제안</Link>}{closed && <Link className="button button-secondary" href={`/meetings/${meetingId}/content/results`}>결과 보기</Link>}</div> : undefined} />
      <MeetingTabs meetingId={meetingId} active="content" />
      {message && <Message tone="success">{message}</Message>}
      {error && <Message tone="error">{error}</Message>}
      {missing && host && (
        <Card>
          <h2>콘텐츠 투표 만들기</h2>
          <form className="row deadline-create-row" onSubmit={createPoll}>
            <DeadlineInput />
            <button className="button button-primary">투표 만들기</button>
          </form>
        </Card>
      )}
      {missing && host === false && <Card><h2>아직 콘텐츠 투표가 없어요</h2><p>호스트가 투표를 만들면 후보를 등록할 수 있습니다.</p></Card>}
      {poll && (
        <div className="stack">
          <div className="poll-status-bar">
            <div><Badge tone={poll.status === "OPEN" ? "green" : "gray"}>{poll.status === "OPEN" ? "투표 진행 중" : "투표 마감"}</Badge><strong>복수 선택 가능</strong></div>
            <span>◷ 마감 {formatDate(poll.deadline)}</span>
          </div>
          {poll.candidates.map((candidate) => {
            const editing = editingId === candidate.candidateId;
            return (
            <Card key={candidate.candidateId} className="poll-option-card">
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
              {!closed && candidate.createdByMemberId !== myMemberId && <small className="muted">모임원이 제안한 후보입니다.</small>}
            </Card>
            );
          })}
          {!closed && <Link className="candidate-cta" href={`/meetings/${meetingId}/content/new`}><span>＋</span><div><strong>새로운 콘텐츠 후보 제안</strong><small>모임원 누구나 활동이나 장소를 등록할 수 있어요.</small></div><b>→</b></Link>}
        </div>
      )}
    </AppShell>
  );
}
