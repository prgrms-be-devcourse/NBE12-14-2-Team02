"use client";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, Message, PageTitle, formatDate } from "@/app/_components/ui";
import { apiFetch, jsonBody } from "@/app/_lib/api";
import type { SchedulePoll } from "@/app/_lib/types";

type Preference = "PREFER" | "AVAILABLE" | "DISLIKE" | "IMPOSSIBLE";
const choices: Array<{ value: Preference; label: string }> = [{ value: "PREFER", label: "선호 3점" }, { value: "AVAILABLE", label: "가능 2점" }, { value: "DISLIKE", label: "별로 1점" }, { value: "IMPOSSIBLE", label: "불가능 0점" }];
export default function ScheduleVotePage() {
  const { meetingId } = useParams<{ meetingId: string }>(); const [poll, setPoll] = useState<SchedulePoll | null>(null); const [selected, setSelected] = useState<Record<number, Preference>>({}); const [message, setMessage] = useState(""); const [error, setError] = useState("");
  useEffect(() => { apiFetch<SchedulePoll>(`/api/meetings/${meetingId}/schedule-poll`).then(setPoll).catch((e) => setError(e.message)); }, [meetingId]);
  async function vote(candidateId: number, preference: Preference) { setSelected((items) => ({ ...items, [candidateId]: preference })); try { await apiFetch(`/api/meetings/${meetingId}/schedule-poll/candidates/${candidateId}/vote`, { method: "PUT", body: jsonBody({ preference }) }); setMessage("선택한 후보의 응답을 저장했습니다."); } catch (e) { setError(e instanceof Error ? e.message : "응답을 저장하지 못했습니다."); } }
  return <AppShell><PageTitle eyebrow="Schedule poll" title="일정 선호도 투표" description="각 날짜 후보에 내 선호도를 한 건씩 저장할 수 있어요." action={<div className="row"><Link className="button button-ghost" href={`/meetings/${meetingId}/schedule/manage`}>후보 관리</Link><Link className="button button-secondary" href={`/meetings/${meetingId}/schedule/results`}>결과 보기</Link></div>} /><MeetingTabs meetingId={meetingId} active="schedule" />{message && <Message tone="success">{message}</Message>}{error && <Message tone="error">{error}</Message>}{poll && <><div className="row between"><Badge tone={poll.status === "OPEN" ? "green" : "gray"}>{poll.status}</Badge><span className="muted">마감 {formatDate(poll.deadline)}</span></div><div className="stack" style={{marginTop:16}}>{poll.candidates.map((candidate) => <Card key={candidate.id}><div className="section-header"><div><p className="eyebrow">Candidate {candidate.id}</p><h2>{candidate.candidateDate}</h2></div><span className="muted">선택 즉시 저장</span></div><div className="choice-grid">{choices.map((choice) => <button disabled={poll.status === "CLOSED"} className={`choice ${selected[candidate.id] === choice.value ? "selected" : ""}`} key={choice.value} onClick={() => vote(candidate.id, choice.value)}>{choice.label}</button>)}</div></Card>)}</div></>}</AppShell>;
}
