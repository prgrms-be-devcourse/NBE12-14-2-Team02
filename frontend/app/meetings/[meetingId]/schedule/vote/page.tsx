"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, Message, PageTitle, formatDate } from "@/app/_components/ui";
import { apiFetch, getCurrentUserId, jsonBody } from "@/app/_lib/api";
import type { SchedulePoll } from "@/app/_lib/types";

type Preference = "PREFER" | "AVAILABLE" | "DISLIKE" | "IMPOSSIBLE";
const choices: Array<{ value: Preference; label: string }> = [
  { value: "PREFER", label: "선호 3점" },
  { value: "AVAILABLE", label: "가능 2점" },
  { value: "DISLIKE", label: "별로 1점" },
  { value: "IMPOSSIBLE", label: "불가능 0점" },
];

function storageKey(pollId: number, userId: number) {
  return `moim.scheduleVotes.${userId}.${pollId}`;
}

function restoreChoices(poll: SchedulePoll, userId: number | null): Record<number, Preference> {
  if (userId === null) return {};
  try {
    const saved = JSON.parse(window.localStorage.getItem(storageKey(poll.id, userId)) || "{}");
    const validValues = new Set(choices.map((choice) => choice.value));
    return Object.fromEntries(
      poll.candidates
        .filter((candidate) => validValues.has(saved[candidate.id]))
        .map((candidate) => [candidate.id, saved[candidate.id] as Preference]),
    );
  } catch {
    return {};
  }
}

export default function ScheduleVotePage() {
  const { meetingId } = useParams<{ meetingId: string }>();
  const [poll, setPoll] = useState<SchedulePoll | null>(null);
  const [selected, setSelected] = useState<Record<number, Preference>>({});
  const [savingId, setSavingId] = useState<number | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const savedRef = useRef<Record<number, Preference>>({});
  const savingRef = useRef(false);

  useEffect(() => {
    let active = true;
    apiFetch<SchedulePoll>(`/api/meetings/${meetingId}/schedule-poll`)
      .then((data) => {
        if (!active) return;
        const restored = restoreChoices(data, getCurrentUserId());
        savedRef.current = restored;
        setSelected(restored);
        setPoll(data);
      })
      .catch((cause) => { if (active) setError(cause instanceof Error ? cause.message : "일정 투표를 불러오지 못했습니다."); });
    return () => { active = false; };
  }, [meetingId]);

  async function vote(candidateId: number, preference: Preference) {
    if (!poll || savingRef.current) return;
    savingRef.current = true;
    setSavingId(candidateId);
    setError("");
    setMessage("");
    try {
      const saved = await apiFetch<{ preference: Preference }>(`/api/meetings/${meetingId}/schedule-poll/candidates/${candidateId}/vote`, {
        method: "PUT",
        body: jsonBody({ preference }),
      });
      const next = { ...savedRef.current, [candidateId]: saved.preference };
      savedRef.current = next;
      setSelected(next);
      const userId = getCurrentUserId();
      if (userId !== null) {
        try { window.localStorage.setItem(storageKey(poll.id, userId), JSON.stringify(next)); }
        catch { /* 저장소가 차단되어도 이번 화면의 선택은 유지한다. */ }
      }
      setMessage("선택한 후보의 응답을 저장했습니다.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "응답을 저장하지 못했습니다.");
    } finally {
      savingRef.current = false;
      setSavingId(null);
    }
  }

  return (
    <AppShell>
      <PageTitle
        eyebrow="Schedule poll"
        title="일정 선호도 투표"
        description="각 날짜 후보에 내 선호도를 한 건씩 저장할 수 있어요."
        action={<div className="row"><Link className="button button-ghost" href={`/meetings/${meetingId}/schedule/manage`}>후보 관리</Link><Link className="button button-secondary" href={`/meetings/${meetingId}/schedule/results`}>결과 보기</Link></div>}
      />
      <MeetingTabs meetingId={meetingId} active="schedule" />
      {message && <Message tone="success">{message}</Message>}
      {error && <Message tone="error">{error}</Message>}
      {poll && <>
        <div className="row between"><Badge tone={poll.status === "OPEN" ? "green" : "gray"}>{poll.status}</Badge><span className="muted">마감 {formatDate(poll.deadline)}</span></div>
        <p className="muted">선택 표시는 이 브라우저에서 성공적으로 저장한 응답입니다. 다른 기기에서 변경한 응답은 표시되지 않을 수 있습니다.</p>
        <div className="stack">
          {poll.candidates.map((candidate) => <Card key={candidate.id}>
            <div className="section-header"><div><p className="eyebrow">Candidate {candidate.id}</p><h2>{candidate.candidateDate}</h2></div><span className="muted">{savingId === candidate.id ? "저장 중…" : selected[candidate.id] ? "저장한 선택" : "이 브라우저에 기록 없음"}</span></div>
            <div className="choice-grid">{choices.map((choice) => <button
              type="button"
              disabled={poll.status === "CLOSED" || savingId !== null}
              className={`choice ${selected[candidate.id] === choice.value ? "selected" : ""}`}
              key={choice.value}
              onClick={() => vote(candidate.id, choice.value)}
            >{choice.label}</button>)}</div>
          </Card>)}
        </div>
      </>}
    </AppShell>
  );
}
