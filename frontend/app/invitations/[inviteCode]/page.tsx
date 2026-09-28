"use client";

import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import AppShell from "@/app/_components/AppShell";
import {
  Badge,
  Card,
  Message,
  PageTitle,
  formatDate,
} from "@/app/_components/ui";
import { apiFetch } from "@/app/_lib/api";
import type {
  MeetingInvitationDetail,
} from "@/app/_lib/types";

export default function InvitationPage() {
  const router = useRouter();

  const { inviteCode } =
      useParams<{ inviteCode: string }>();

  const [invitation, setInvitation] =
      useState<MeetingInvitationDetail | null>(null);

  const [error, setError] = useState("");

  useEffect(() => {
    loadInvitation();
  }, [inviteCode]);

  async function loadInvitation() {
    try {
      const data =
          await apiFetch<MeetingInvitationDetail>(
              `/api/invitations/${inviteCode}`
          );

      setInvitation(data);
    } catch (error) {
      setError(
          error instanceof Error
              ? error.message
              : "초대 정보를 불러오지 못했습니다."
      );
    }
  }

  async function handleJoin() {
    if (!invitation) {
      return;
    }

    try {
      await apiFetch(
          `/api/invitations/${inviteCode}/join`,
          {
            method: "POST",
          }
      );

      goToMeeting();
    } catch (error) {
      setError(
          error instanceof Error
              ? error.message
              : "모임에 참여하지 못했습니다."
      );
    }
  }

  function handleDecline() {
    router.push("/");
  }

  function goToMeeting() {
    if (!invitation) {
      return;
    }

    router.push(
        `/meetings/${invitation.meetingId}`
    );
  }

  return (
      <AppShell>
        <PageTitle
            eyebrow="Invitation"
            title="모임 초대"
            description="초대 정보를 확인하고 모임에 참여하세요."
        />

        {error && (
            <Message tone="error">
              {error}
            </Message>
        )}

        {invitation && (
            <Card className="stack">
              <InvitationHeader
                  invitation={invitation}
              />

              <InvitationInfo
                  invitation={invitation}
              />

              {invitation.alreadyJoined ? (
                  <AlreadyJoinedActions
                      onGoToMeeting={goToMeeting}
                  />
              ) : (
                  <JoinActions
                      onJoin={handleJoin}
                      onDecline={handleDecline}
                  />
              )}
            </Card>
        )}
      </AppShell>
  );
}

function InvitationHeader({
                            invitation,
                          }: {
  invitation: MeetingInvitationDetail;
}) {
  return (
      <div className="row between">
        <h2>{invitation.meetingName}</h2>

        <Badge
            tone={
              invitation.alreadyJoined
                  ? "green"
                  : "purple"
            }
        >
          {invitation.alreadyJoined
              ? "참여 중"
              : "초대됨"}
        </Badge>
      </div>
  );
}

function InvitationInfo({
                          invitation,
                        }: {
  invitation: MeetingInvitationDetail;
}) {
  return (
      <div className="meta">
      <span>
        모임 ID #{invitation.meetingId}
      </span>

        <span>
        참여자 {invitation.participantCount}명
      </span>

        <span>
        만료 {formatDate(invitation.expiresAt)}
      </span>
      </div>
  );
}

function AlreadyJoinedActions({
                                onGoToMeeting,
                              }: {
  onGoToMeeting: () => void;
}) {
  return (
      <>
        <Message tone="success">
          이미 참여 중인 모임입니다.
        </Message>

        <button
            className="button button-primary"
            onClick={onGoToMeeting}
        >
          모임 바로가기
        </button>
      </>
  );
}

function JoinActions({
                       onJoin,
                       onDecline,
                     }: {
  onJoin: () => void;
  onDecline: () => void;
}) {
  return (
      <>
        <Message>
          이 모임에 참여하시겠습니까?
        </Message>

        <div className="row">
          <button
              className="button button-primary"
              onClick={onJoin}
          >
            모임 참여하기
          </button>

          <button
              className="button button-ghost"
              onClick={onDecline}
          >
            참여하지 않기
          </button>
        </div>
      </>
  );
}