"use client";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, Message, PageTitle } from "@/app/_components/ui";
import { apiFetch, jsonBody } from "@/app/_lib/api";
import type { ContentResults } from "@/app/_lib/types";

const label: Record<string, string> = { PREFER: "선호", AVAILABLE: "가능", DISLIKE: "별로" };
export default function ContentResultsPage() {
  const { meetingId } = useParams<{ meetingId: string }>(); const [result, setResult] = useState<ContentResults | null>(null); const [error, setError] = useState("");
  const load = useCallback(() => apiFetch<ContentResults>(`/api/meetings/${meetingId}/content-poll/results?sort=SCORE`).then(setResult).catch((e) => setError(e.message)), [meetingId]);
  useEffect(() => { void load(); }, [load]);
  async function confirm(candidateId: number) { try { await apiFetch(`/api/meetings/${meetingId}/content-poll/confirm`, { method: "POST", body: jsonBody({ candidateId }) }); load(); } catch (e) { setError(e instanceof Error ? e.message : "확정하지 못했습니다."); } }
  return <AppShell><PageTitle eyebrow="Closed poll" title="콘텐츠 투표 결과" description="마감된 후보의 점수와 참여자별 응답을 확인하세요." /><MeetingTabs meetingId={meetingId} active="content" />{error && <Message tone="error">{error}</Message>}{result && <div className="stack"><div className="row between"><Badge tone="gray">{result.status}</Badge><span className="muted">참여 인원 {result.joinedCount}명</span></div><div className="grid grid-2">{result.candidates.map((candidate, index) => <Card key={candidate.candidateId}><div className="row between"><Badge>{index + 1}위</Badge>{result.confirmedCandidateId === candidate.candidateId && <Badge tone="green">확정</Badge>}</div><h2>{candidate.title}</h2><p>{candidate.description}</p><strong>{candidate.totalScore}점</strong><div className="meta"><span>선호 {candidate.preferCount}</span><span>가능 {candidate.availableCount}</span><span>별로 {candidate.dislikeCount}</span><span>미응답 {candidate.noResponseCount}</span></div>{result.confirmedCandidateId === null && <button className="button button-primary" onClick={() => confirm(candidate.candidateId)}>이 후보로 확정하기</button>}</Card>)}</div><Card><h2>참여자별 응답</h2><div className="table-wrap"><table><thead><tr><th>참여자</th>{result.candidates.map((candidate) => <th key={candidate.candidateId}>{candidate.title}</th>)}<th>응답 수</th></tr></thead><tbody>{result.members.map((member) => <tr key={member.meetingMemberId}><td>{member.nickname}{member.host ? " (호스트)" : ""}</td>{member.preferences.map((preference, index) => <td key={index}>{preference ? label[preference] : "미응답"}</td>)}<td>{member.responseCount}</td></tr>)}</tbody></table></div></Card></div>}</AppShell>;
}
