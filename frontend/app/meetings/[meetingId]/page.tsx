"use client";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, EmptyState, Message, PageTitle } from "@/app/_components/ui";
import { apiFetch } from "@/app/_lib/api";
import type { Meeting, MeetingMember } from "@/app/_lib/types";

type Invitation = { id: number; meetingId: number; inviteCode: string; expiresAt: string };
export default function MeetingDetailPage() {
  const { meetingId } = useParams<{ meetingId: string }>(); const [meeting, setMeeting] = useState<Meeting | null>(null); const [members, setMembers] = useState<MeetingMember[]>([]); const [invitations, setInvitations] = useState<Invitation[]>([]); const [error, setError] = useState("");
  useEffect(() => { Promise.all([apiFetch<Meeting>(`/api/meetings/${meetingId}`), apiFetch<MeetingMember[]>(`/api/meetings/${meetingId}/members`)]).then(([m, list]) => { setMeeting(m); setMembers(list); }).catch((e) => setError(e.message)); apiFetch<Invitation[]>(`/api/meetings/${meetingId}/invitations`).then(setInvitations).catch(() => undefined); }, [meetingId]);
  async function createInvitation() { try { const invitation = await apiFetch<Invitation>(`/api/meetings/${meetingId}/invitations`, { method: "POST" }); setInvitations((items) => [...items, invitation]); } catch (e) { setError(e instanceof Error ? e.message : "초대를 만들지 못했습니다."); } }
  const inviteUrl = invitations.at(-1) ? `${window.location.origin}/invitations/${invitations.at(-1)?.inviteCode}` : "";
  return <AppShell>{error && <Message tone="error">{error}</Message>}{meeting && <><PageTitle eyebrow={`Meeting #${meeting.id}`} title={meeting.name} description={meeting.description || "모임 설명이 없습니다."} action={<Badge tone={meeting.status === "ACTIVE" ? "green" : "gray"}>{meeting.status}</Badge>} /><div className="meta"><span>호스트 ID {meeting.hostId}</span><span>참여자 {meeting.participantCount}명</span></div><MeetingTabs meetingId={meetingId} active="overview" /><div className="grid grid-2"><Card><div className="section-header"><h2>초대 링크</h2><button className="button button-secondary button-small" onClick={createInvitation}>새 링크 생성</button></div>{inviteUrl ? <div className="row"><input className="input" readOnly value={inviteUrl} /><button className="button button-ghost" onClick={() => navigator.clipboard.writeText(inviteUrl)}>복사</button></div> : <EmptyState title="활성 초대 링크가 없어요" description="호스트가 새 링크를 만들 수 있습니다." />}</Card><Card><h2>참여 멤버</h2><div className="list">{members.map((member) => <div className="list-item" key={member.id}><div><strong>{member.nickname}</strong><p>사용자 #{member.userId}</p></div><Badge tone={member.status === "JOINED" ? "green" : "gray"}>{member.status}</Badge></div>)}</div></Card></div></>}</AppShell>;
}
