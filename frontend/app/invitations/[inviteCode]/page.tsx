"use client";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import AppShell from "@/app/_components/AppShell";
import { Badge, Card, Message, PageTitle, formatDate } from "@/app/_components/ui";
import { apiFetch } from "@/app/_lib/api";

type Invitation = { inviteCode: string; meetingId: number; meetingName: string; expiresAt: string; alreadyJoined: boolean };
export default function InvitationPage() {
  const { inviteCode } = useParams<{ inviteCode: string }>(); const router = useRouter(); const [invitation, setInvitation] = useState<Invitation | null>(null); const [error, setError] = useState("");
  useEffect(() => { apiFetch<Invitation>(`/api/invitations/${inviteCode}`).then(setInvitation).catch((e) => setError(e.message)); }, [inviteCode]);
  async function join() { try { await apiFetch(`/api/invitations/${inviteCode}/join`, { method: "POST" }); router.push(`/meetings/${invitation?.meetingId}`); } catch (e) { setError(e instanceof Error ? e.message : "참여하지 못했습니다."); } }
  return <AppShell><PageTitle eyebrow="Invitation" title="모임 초대" description="초대 정보를 확인하고 모임에 참여하세요." />{error && <Message tone="error">{error}</Message>}{invitation && <Card className="stack"><div className="row between"><h2>{invitation.meetingName}</h2><Badge tone={invitation.alreadyJoined ? "green" : "purple"}>{invitation.alreadyJoined ? "참여 중" : "초대됨"}</Badge></div><div className="meta"><span>모임 ID #{invitation.meetingId}</span><span>만료 {formatDate(invitation.expiresAt)}</span></div><p className="muted">초대 코드 {invitation.inviteCode}</p><button className="button button-primary" disabled={invitation.alreadyJoined} onClick={join}>{invitation.alreadyJoined ? "이미 참여한 모임입니다" : "모임 참여하기"}</button></Card>}</AppShell>;
}
