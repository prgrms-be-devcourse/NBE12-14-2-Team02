"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import AppShell from "@/app/_components/AppShell";
import {
  Badge,
  Card,
  EmptyState,
  Message,
  PageTitle,
} from "@/app/_components/ui";
import { apiFetch } from "@/app/_lib/api";
import type { Meeting } from "@/app/_lib/types";

export default function Home() {
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadMeetings() {
      try {
        const data = await apiFetch<Meeting[]>("/api/meetings");
        setMeetings(data);
      } catch (error) {
        setError(
            error instanceof Error
                ? error.message
                : "모임 목록을 불러오지 못했습니다."
        );
      }
    }

    void loadMeetings();
  }, []);

  return (
      <AppShell>
        <PageTitle
            eyebrow="My meetings"
            title="내 모임"
            description="참여 중인 모임을 확인하고 새로운 모임을 시작하세요."
            action={
              <Link
                  href="/meetings/new"
                  className="button button-primary"
              >
                + 새 모임 만들기
              </Link>
            }
        />

        {error && (
            <Message tone="error">
              {error}
            </Message>
        )}

        <div className="grid grid-3">
          {meetings.map((meeting) => (
              <MeetingCard
                  key={meeting.id}
                  meeting={meeting}
              />
          ))}
        </div>

        {!error && meetings.length === 0 && (
            <Card>
              <EmptyState
                  title="참여 중인 모임이 없어요"
                  description="새 모임을 만들거나 초대 링크로 참여해 보세요."
              />
            </Card>
        )}
      </AppShell>
  );
}

function MeetingCard({ meeting }: { meeting: Meeting }) {
  const isActive = meeting.status === "ACTIVE";

  return (
      <Card className="meeting-card">
        <Badge tone={isActive ? "green" : "gray"}>
          {meeting.status}
        </Badge>

        <h2>{meeting.name}</h2>

        <p>
          {meeting.description || "등록된 설명이 없습니다."}
        </p>

        <p className="muted">
          참여자 {meeting.participantCount}명
        </p>

        <Link
            className="button button-secondary"
            href={`/meetings/${meeting.id}`}
        >
          상세보기
        </Link>
      </Card>
  );
}