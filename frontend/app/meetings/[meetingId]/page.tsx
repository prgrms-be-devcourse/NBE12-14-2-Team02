"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";

import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import {
  Badge,
  Card,
  EmptyState,
  Message,
  PageTitle,
} from "@/app/_components/ui";
import { apiFetch } from "@/app/_lib/api";

import type {
  Meeting,
  MeetingInvitation,
  MeetingMember,
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

        setInvitations(data);
      } catch {
        // 초대 링크 조회 실패가
        // 모임 상세 조회 자체를 막지는 않음
      }
    }

    void loadMeeting();
    void loadInvitations();
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

  const latestInvitation =
      invitations.at(-1);

  const inviteUrl = latestInvitation
      ? `${window.location.origin}/invitations/${latestInvitation.inviteCode}`
      : "";

  return (
      <AppShell>
        {error && (
            <Message tone="error">
              {error}
            </Message>
        )}

        <MeetingHeader meeting={meeting} />

        <MeetingTabs
            meetingId={meetingId}
            active="overview"
        />

        <div className="grid grid-2">
          <InvitationSection
              inviteUrl={inviteUrl}
              onCreateInvitation={
                handleCreateInvitation
              }
          />

          <MemberSection members={members} />
        </div>
      </AppShell>
  );
}

function MeetingHeader({
                         meeting,
                       }: {
  meeting: Meeting;
}) {
  const isActive =
      meeting.status === "ACTIVE";

  return (
      <>
        <PageTitle
            eyebrow={`Meeting #${meeting.id}`}
            title={meeting.name}
            description={
                meeting.description ||
                "모임 설명이 없습니다."
            }
            action={
              <Badge
                  tone={isActive ? "green" : "gray"}
              >
                {meeting.status}
              </Badge>
            }
        />

        <div className="meta">
        <span>
          호스트 ID {meeting.hostId}
        </span>

          <span>
          참여자 {meeting.participantCount}명
        </span>
        </div>
      </>
  );
}

function InvitationSection({
                             inviteUrl,
                             onCreateInvitation,
                           }: {
  inviteUrl: string;
  onCreateInvitation: () => void;
}) {
  async function copyInvitation() {
    await navigator.clipboard.writeText(
        inviteUrl
    );
  }

  return (
      <Card>
        <div className="section-header">
          <h2>초대 링크</h2>

          <button
              className="button button-secondary button-small"
              onClick={onCreateInvitation}
          >
            새 링크 생성
          </button>
        </div>

        {inviteUrl ? (
            <div className="row">
              <input
                  className="input"
                  readOnly
                  value={inviteUrl}
              />

              <button
                  className="button button-ghost"
                  onClick={copyInvitation}
              >
                복사
              </button>
            </div>
        ) : (
            <EmptyState
                title="활성 초대 링크가 없어요"
                description="호스트가 새 링크를 만들 수 있습니다."
            />
        )}
      </Card>
  );
}

function MemberSection({
                         members,
                       }: {
  members: MeetingMember[];
}) {
  return (
      <Card>
        <h2>참여 멤버</h2>

        <div className="list">
          {members.map((member) => (
              <MemberItem
                  key={member.id}
                  member={member}
              />
          ))}
        </div>
      </Card>
  );
}

function MemberItem({
                      member,
                    }: {
  member: MeetingMember;
}) {
  const isJoined =
      member.status === "JOINED";

  return (
      <div className="list-item">
        <div>
          <strong>{member.nickname}</strong>

          <p>사용자 #{member.userId}</p>
        </div>

        <Badge
            tone={isJoined ? "green" : "gray"}
        >
          {member.status}
        </Badge>
      </div>
  );
}