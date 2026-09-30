"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import AppShell from "@/app/_components/AppShell";
import DeadlineInput from "@/app/_components/DeadlineInput";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Card, EmptyState, Message, PageTitle } from "@/app/_components/ui";
import { ApiError, apiFetch, getCurrentUserId, jsonBody } from "@/app/_lib/api";
import { parseDeadline } from "@/app/_lib/deadline";
import type { Meeting, SchedulePoll } from "@/app/_lib/types";

export default function SchedulePage() {
  const { meetingId } = useParams<{ meetingId: string }>();
  const router = useRouter();
  const [state, setState] = useState<"loading" | "missing" | "error">("loading");
  const [meetingActive, setMeetingActive] = useState(true);
  const [canCreate, setCanCreate] = useState(false);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const meeting = await apiFetch<Meeting>(`/api/meetings/${meetingId}`);
        if (cancelled) return;
        const active = meeting.status === "ACTIVE";
        setMeetingActive(active);
        setCanCreate(active && meeting.hostId === getCurrentUserId());

        try {
          const poll = await apiFetch<SchedulePoll>(`/api/meetings/${meetingId}/schedule-poll`);
          if (cancelled) return;
          router.replace(`/meetings/${meetingId}/schedule/${poll.status === "CLOSED" ? "results" : "vote"}`);
        } catch (cause) {
          if (cancelled) return;
          if (cause instanceof ApiError && cause.status === 404) {
            setState("missing");
          } else {
            setError(cause instanceof Error ? cause.message : "일정 투표를 불러오지 못했습니다.");
            setState("error");
          }
        }
      } catch (cause) {
        if (cancelled) return;
        setError(cause instanceof Error ? cause.message : "모임을 불러오지 못했습니다.");
        setState("error");
      }
    }

    void load();
    return () => { cancelled = true; };
  }, [meetingId, router]);

  async function createPoll(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCreating(true);
    setError("");
    try {
      const deadline = parseDeadline(new FormData(event.currentTarget).get("deadline"));
      await apiFetch(`/api/meetings/${meetingId}/schedule-poll`, {
        method: "POST",
        body: jsonBody({ deadline }),
      });
      router.replace(`/meetings/${meetingId}/schedule/manage`);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "일정 투표를 만들지 못했습니다.");
      setCreating(false);
    }
  }

  return (
    <AppShell>
      <PageTitle
        eyebrow="Schedule poll"
        title="일정 투표"
        description="모임의 날짜 후보를 정하고 선호도를 입력하세요."
      />
      <MeetingTabs meetingId={meetingId} active="schedule" />

      {state === "loading" && <Card><Message>일정 투표를 확인하고 있습니다.</Message></Card>}

      {state === "missing" && (
        <Card>
          {canCreate ? (
            <>
              <h2>일정 투표 만들기</h2>
              <form className="row deadline-create-row" onSubmit={createPoll}>
                <DeadlineInput />
                <button className="button button-primary" type="submit" disabled={creating}>
                  {creating ? "만드는 중..." : "투표 만들기"}
                </button>
              </form>
              <p className="muted">투표를 만든 다음 날짜 후보를 추가할 수 있습니다.</p>
            </>
          ) : (
            <EmptyState
              title={meetingActive ? "아직 일정 투표가 열리지 않았어요" : "종료된 모임입니다"}
              description={meetingActive
                ? "모임장이 일정 투표를 만들면 날짜 후보를 확인할 수 있습니다."
                : "종료된 모임에는 새 일정 투표를 만들 수 없습니다."}
            />
          )}
        </Card>
      )}

      {error && <Message tone="error">{error}</Message>}
      {state === "error" && (
        <Link className="button button-secondary" href={`/meetings/${meetingId}`}>
          모임으로 돌아가기
        </Link>
      )}
    </AppShell>
  );
}
