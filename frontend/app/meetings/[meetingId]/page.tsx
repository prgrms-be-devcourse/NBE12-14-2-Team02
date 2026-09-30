"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";

import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import {
  Badge,
  Card,
  EmptyState,
  Message,
} from "@/app/_components/ui";
import { apiFetch } from "@/app/_lib/api";
import type { ExpenseList } from "@/app/_lib/expenses";

import type {
  Meeting,
  MeetingInvitation,
  MeetingMember,
  SchedulePoll,
  ContentPoll,
} from "@/app/_lib/types";

export default function MeetingDetailPage() {
  const { meetingId } =
      useParams<{ meetingId: string }>();

  const [meeting, setMeeting] =
      useState<Meeting | null>(null);

  const [members, setMembers] =
      useState<MeetingMember[]>([]);

  const [invitations, setInvitations] =
      useState<MeetingInvitation[]>([]);

  const [schedulePoll, setSchedulePoll] = useState<SchedulePoll | null>(null);
  const [contentPoll, setContentPoll] = useState<ContentPoll | null>(null);
  const [expenseList, setExpenseList] = useState<ExpenseList | null>(null);

  const [error, setError] = useState("");

  useEffect(() => {
    async function loadMeeting() {
      try {
        const [meetingData, memberData] =
            await Promise.all([
              apiFetch<Meeting>(
                  `/api/meetings/${meetingId}`
              ),
              apiFetch<MeetingMember[]>(
                  `/api/meetings/${meetingId}/members`
              ),
            ]);

        setMeeting(meetingData);
        setMembers(memberData);
      } catch (error) {
        setError(
            error instanceof Error
                ? error.message
                : "모임 정보를 불러오지 못했습니다."
        );
      }
    }

    async function loadInvitations() {
      try {
        const data =
            await apiFetch<MeetingInvitation[]>(
                `/api/meetings/${meetingId}/invitations`
            );

        setInvitations(data.filter(
            (invitation) => new Date(invitation.expiresAt).getTime() > Date.now()
        ));
      } catch {
        // 초대 링크 조회 실패가
        // 모임 상세 조회 자체를 막지는 않음
      }
    }

    void loadMeeting();
    void loadInvitations();
    void Promise.allSettled([
      apiFetch<SchedulePoll>(`/api/meetings/${meetingId}/schedule-poll`).then(setSchedulePoll),
      apiFetch<ContentPoll>(`/api/meetings/${meetingId}/content-poll`).then(setContentPoll),
      apiFetch<ExpenseList>(`/api/meetings/${meetingId}/expenses`).then(setExpenseList),
    ]);
  }, [meetingId]);

  async function handleCreateInvitation() {
    try {
      const invitation =
          await apiFetch<MeetingInvitation>(
              `/api/meetings/${meetingId}/invitations`,
              {
                method: "POST",
              }
          );

      setInvitations((current) => [
        ...current,
        invitation,
      ]);
    } catch (error) {
      setError(
          error instanceof Error
              ? error.message
              : "초대 링크를 만들지 못했습니다."
      );
    }
  }

  if (!meeting) {
    return (
        <AppShell>
          {error && (
              <Message tone="error">
                {error}
              </Message>
          )}
        </AppShell>
    );
  }

  const latestInvitation = invitations.at(-1);

  const inviteUrl = latestInvitation
      ? `${window.location.origin}/invitations/${latestInvitation.inviteCode}`
      : "";

  const hostNickname = members.find(
      (member) => member.userId === meeting.hostId
  )?.nickname;

  return (
      <AppShell>
        {error && (
            <Message tone="error">
              {error}
            </Message>
        )}

        <nav className="breadcrumb" aria-label="현재 위치">
          <Link href="/">내 모임</Link>
          <span aria-hidden="true">›</span>
          <span>{meeting.name}</span>
        </nav>

        <MeetingHeader
            meeting={meeting}
            hostNickname={hostNickname}
        />

        <MeetingTabs
            meetingId={meetingId}
            active="overview"
        />

        <div className="overview-grid feature-overview-grid">
          <FeatureCard
            icon="◷"
            title="일정 조율"
            status={schedulePoll ? (schedulePoll.status === "OPEN" ? "투표 진행 중" : "투표 마감") : "투표 준비 전"}
            href={`/meetings/${meetingId}/schedule`}
            action={schedulePoll?.status === "OPEN" ? "일정 투표하기" : "일정 확인하기"}
          />
          <FeatureCard
            icon="◇"
            title="콘텐츠 투표"
            status={contentPoll ? (contentPoll.status === "OPEN" ? "투표 진행 중" : "투표 마감") : "투표 준비 전"}
            href={`/meetings/${meetingId}/content`}
            action="콘텐츠 투표하기"
          />
          <FeatureCard
            icon="₩"
            title="지출 · 정산"
            status={expenseList?.editable === false ? "읽기 전용" : "정산 진행 가능"}
            href={`/meetings/${meetingId}/settlement`}
            action="지출 내역 확인"
          />
        </div>

        <div className="overview-grid">
          <InvitationSection
              inviteUrl={inviteUrl}
              expiresAt={latestInvitation?.expiresAt}
              onCreateInvitation={
                handleCreateInvitation
              }
          />
          <MemberSection members={members} hostId={meeting.hostId} />
        </div>
      </AppShell>
  );
}

function FeatureCard({ icon, title, status, href, action }: { icon: string; title: string; status: string; href: string; action: string }) {
  return <Card className="feature-card">
    <div className="row between"><span className="feature-icon" aria-hidden="true">{icon}</span><Badge tone={status.includes("진행") || status.includes("가능") ? "green" : "gray"}>{status}</Badge></div>
    <h2>{title}</h2>
    <Link className="feature-link" href={href}>{action} <span aria-hidden="true">→</span></Link>
  </Card>;
}

function MeetingHeader({
                         meeting,
                         hostNickname,
                       }: {
  meeting: Meeting;
  hostNickname?: string;
}) {
  const isActive =
      meeting.status === "ACTIVE";

  return (
      <section className="meeting-hero">
        <div className="meeting-hero-main">
          <div className="meeting-hero-heading">
            <h1>{meeting.name}</h1>
            <Badge tone={isActive ? "purple" : "gray"}>
              {isActive ? "진행 중" : "완료"}
            </Badge>
          </div>
          <p>{meeting.description || "모임 설명이 없습니다."}</p>
        </div>
        <div className="meeting-hero-meta">
          <div><span>호스트</span><strong>{hostNickname || "확인 중"}</strong></div>
        </div>
      </section>
  );
}

function InvitationSection({
                             inviteUrl,
                             expiresAt,
                             onCreateInvitation,
                           }: {
  inviteUrl: string;
  expiresAt?: string;
  onCreateInvitation: () => void;
}) {
  const [copied, setCopied] = useState(false);

  async function copyInvitation() {
    try {
      await navigator.clipboard.writeText(inviteUrl);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  }

  return (
      <Card className="overview-card">
        <div className="overview-card-heading">
          <h2>초대 링크 관리</h2>
        </div>
        <p>초대 링크를 생성하고 공유하여 새로운 모임원을 초대할 수 있습니다.</p>

        {inviteUrl ? (
            <div className="invite-link-box">
              <div className="invite-link-status">
                <span>현재 유효한 초대 링크</span>
                <strong>유효함</strong>
              </div>
              <div className="invite-link-row">
                <input className="input" aria-label="초대 링크" readOnly value={inviteUrl} />
                <button className="button button-primary button-small" onClick={copyInvitation}>
                  {copied ? "복사 완료" : "복사"}
                </button>
              </div>
              {expiresAt && <small>만료: {new Date(expiresAt).toLocaleString("ko-KR")}</small>}
            </div>
        ) : (
            <EmptyState
                title="활성 초대 링크가 없어요"
                description="호스트가 새 링크를 만들 수 있습니다."
            />
        )}
        <div className="invite-actions">
          <span>새로운 초대 코드가 필요하신가요?</span>
          <button className="button button-secondary button-small" onClick={onCreateInvitation}>
            {inviteUrl ? "초대 링크 재생성" : "초대 링크 생성"}
          </button>
        </div>
      </Card>
  );
}

function MemberSection({
                         members,
                         hostId,
                       }: {
  members: MeetingMember[];
  hostId: number;
}) {
  return (
      <Card className="overview-card">
        <div className="overview-card-heading">
          <h2>모임원 목록 <span>{members.length}명</span></h2>
        </div>

        <div className="member-list">
          {members.map((member) => (
              <MemberItem
                  key={member.id}
                  member={member}
                  isHost={member.userId === hostId}
              />
          ))}
        </div>
      </Card>
  );
}

function MemberItem({
                      member,
                      isHost,
                    }: {
  member: MeetingMember;
  isHost: boolean;
}) {
  const isJoined =
      member.status === "JOINED";

  return (
      <div className="member-item">
        <div className="member-identity">
          <span className="member-avatar" aria-hidden="true">{member.nickname.charAt(0)}</span>
          <strong>{member.nickname}</strong>
        </div>
        <Badge tone={isHost ? "purple" : "gray"}>
          {isHost ? "호스트" : isJoined ? "모임원" : "탈퇴"}
        </Badge>
      </div>
  );
}
