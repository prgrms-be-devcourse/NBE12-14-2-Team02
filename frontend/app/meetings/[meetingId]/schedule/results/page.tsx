"use client";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, Message, PageTitle } from "@/app/_components/ui";
import { apiFetch } from "@/app/_lib/api";
import type { ScheduleResult } from "@/app/_lib/types";

const label: Record<string, string> = { PREFER: "선호", AVAILABLE: "가능", DISLIKE: "별로", IMPOSSIBLE: "불가능" };
export default function ScheduleResultsPage() {
  const { meetingId } = useParams<{ meetingId: string }>(); const [result, setResult] = useState<ScheduleResult | null>(null); const [error, setError] = useState("");
  useEffect(() => { apiFetch<ScheduleResult>(`/api/meetings/${meetingId}/schedule-poll/results`).then(setResult).catch((e) => setError(e.message)); }, [meetingId]);
  return <AppShell><PageTitle eyebrow="Closed poll" title="일정 투표 결과" description="마감된 투표의 순위와 참여자별 응답입니다." /><MeetingTabs meetingId={meetingId} active="schedule" />{error && <Message tone="error">{error}</Message>}{result && <div className="stack"><div className="grid grid-2">{result.candidateRanks.map((candidate) => <Card key={candidate.candidateId}><div className="row between"><Badge tone={candidate.rank === 1 ? "purple" : "gray"}>{candidate.rank}위</Badge><strong>{candidate.totalScore}점</strong></div><h2>{candidate.candidateDate}</h2><p>응답 {candidate.responseCount}명 · 미응답 {candidate.nonResponseCount}명</p><div className="meta"><span>선호 {candidate.preferenceCounts.prefer}</span><span>가능 {candidate.preferenceCounts.available}</span><span>별로 {candidate.preferenceCounts.dislike}</span><span>불가능 {candidate.preferenceCounts.impossible}</span></div></Card>)}</div><Card><h2>참여자별 응답</h2><div className="table-wrap"><table><thead><tr><th>참여자</th>{result.candidateRanks.map((item) => <th key={item.candidateId}>{item.candidateDate}</th>)}</tr></thead><tbody>{result.participantResponses.map((member) => <tr key={member.meetingMemberId}><td>{member.nickname}</td>{member.answers.map((answer) => <td key={answer.candidateId}>{answer.preference ? label[answer.preference] : "미응답"}</td>)}</tr>)}</tbody></table></div></Card></div>}</AppShell>;
}
