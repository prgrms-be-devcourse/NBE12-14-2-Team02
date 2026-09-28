"use client";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, Message, PageTitle } from "@/app/_components/ui";
import { apiFetch, getCurrentUserId, jsonBody } from "@/app/_lib/api";
import type { ContentResults } from "@/app/_lib/types";

const label: Record<string, string> = { PREFER: "선호", AVAILABLE: "가능", DISLIKE: "별로" };
const WINDOW = 4;

export default function ContentResultsPage() {
  const { meetingId } = useParams<{ meetingId: string }>();
  const [result, setResult] = useState<ContentResults | null>(null);
  const [error, setError] = useState("");
  const [start, setStart] = useState(0);
  const load = useCallback(() => apiFetch<ContentResults>(`/api/meetings/${meetingId}/content-poll/results?sort=SCORE`).then(setResult).catch((e) => setError(e.message)), [meetingId]);
  useEffect(() => { void load(); }, [load]);
  useEffect(() => {
    if (!result) return;
    setStart((current) => Math.min(current, Math.max(0, result.candidates.length - WINDOW)));
  }, [result]);

  async function confirm(candidateId: number) {
    try {
      await apiFetch(`/api/meetings/${meetingId}/content-poll/confirm`, { method: "POST", body: jsonBody({ candidateId }) });
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "확정하지 못했습니다.");
    }
  }

  const candidates = result?.candidates ?? [];
  const visible = candidates.slice(start, start + WINDOW);
  const paged = candidates.length > WINDOW;
  const canPrev = start > 0;
  const canNext = start + WINDOW < candidates.length;

  function shift(delta: number) {
    setStart((current) => {
      const max = Math.max(0, candidates.length - WINDOW);
      return Math.min(max, Math.max(0, current + delta));
    });
  }

  return (
    <AppShell>
      <PageTitle eyebrow="Closed poll" title="콘텐츠 투표 결과" description="마감된 후보의 점수와 참여자별 응답을 확인하세요." />
      <MeetingTabs meetingId={meetingId} active="content" />
      {error && <Message tone="error">{error}</Message>}
      {result && (
        <div className="stack">
          <div className="row between">
            <Badge tone="gray">{result.status}</Badge>
            <span className="muted">참여 인원 {result.joinedCount}명</span>
          </div>
          <div className="grid grid-2">
            {result.candidates.map((candidate, index) => (
              <Card key={candidate.candidateId}>
                <div className="row between">
                  <Badge>{index + 1}위</Badge>
                  {result.confirmedCandidateId === candidate.candidateId && <Badge tone="green">확정</Badge>}
                </div>
                <h2>{candidate.title}</h2>
                <p>{candidate.description}</p>
                <strong>{candidate.totalScore}점</strong>
                <div className="meta">
                  <span>선호 {candidate.preferCount}</span>
                  <span>가능 {candidate.availableCount}</span>
                  <span>별로 {candidate.dislikeCount}</span>
                  <span>미응답 {candidate.noResponseCount}</span>
                </div>
                {result.status === "CLOSED" && result.confirmedCandidateId === null && result.members.some((member) => member.host && member.userId === getCurrentUserId()) && (
                  <button className="button button-primary" onClick={() => confirm(candidate.candidateId)}>이 후보로 확정하기</button>
                )}
              </Card>
            ))}
          </div>
          <Card>
            <h2>참여자별 응답</h2>
            <div className="table-wrap">
              <table className="response-table">
                <thead>
                  <tr>
                    <th className="name-col">참여자</th>
                    {paged && <th className="arrow-col"><button className="button button-ghost button-small" type="button" aria-label="이전 후보" disabled={!canPrev} onClick={() => shift(-1)}>‹</button></th>}
                    {visible.map((candidate) => <th key={candidate.candidateId}>{candidate.title}</th>)}
                    {paged && <th className="arrow-col"><button className="button button-ghost button-small" type="button" aria-label="다음 후보" disabled={!canNext} onClick={() => shift(1)}>›</button></th>}
                    <th className="count-col">응답 수</th>
                  </tr>
                </thead>
                <tbody>
                  {result.members.map((member) => (
                    <tr key={member.meetingMemberId}>
                      <td className="name-col">{member.nickname}{member.host ? " (호스트)" : ""}</td>
                      {paged && <td className="arrow-col" />}
                      {member.preferences.slice(start, start + WINDOW).map((preference, index) => <td key={visible[index]?.candidateId ?? index}>{preference ? label[preference] : "미응답"}</td>)}
                      {paged && <td className="arrow-col" />}
                      <td className="count-col">{member.responseCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
        </div>
      )}
    </AppShell>
  );
}
