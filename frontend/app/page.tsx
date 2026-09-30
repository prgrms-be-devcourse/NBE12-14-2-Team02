"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
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
  const router = useRouter();
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [filter, setFilter] = useState<"ALL" | "ACTIVE" | "COMPLETED">("ALL");
  const [inviteUrl, setInviteUrl] = useState("");
  const [inviteError, setInviteError] = useState("");
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

  const activeCount = meetings.filter((meeting) => meeting.status === "ACTIVE").length;
  const completedCount = meetings.length - activeCount;
  const visibleMeetings = filter === "ALL" ? meetings : meetings.filter((meeting) => meeting.status === filter);

  function openInvite() {
    setInviteError("");
    const match = inviteUrl.trim().match(/\/invitations\/([^/?#]+)/);
    if (!match) {
      setInviteError("올바른 MOIM 초대 링크를 입력해주세요.");
      return;
    }
    router.push(`/invitations/${encodeURIComponent(match[1])}`);
  }

  return (
      <AppShell>
        <PageTitle
            eyebrow="My meetings"
            title="내 모임 대시보드"
            description="참여 중인 모임을 확인하고 새로운 모임을 시작해 보세요."
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

        <div className="grid grid-3 dashboard-stats">
          <button className={`stat-card ${filter === "ALL" ? "active" : ""}`} onClick={() => setFilter("ALL")}>
            <span>전체 모임</span><strong>{meetings.length}</strong><small>참여 중인 모든 모임</small>
          </button>
          <button className={`stat-card ${filter === "ACTIVE" ? "active" : ""}`} onClick={() => setFilter("ACTIVE")}>
            <span>진행 중</span><strong>{activeCount}</strong><small>투표와 정산 진행 가능</small>
          </button>
          <button className={`stat-card ${filter === "COMPLETED" ? "active" : ""}`} onClick={() => setFilter("COMPLETED")}>
            <span>완료</span><strong>{completedCount}</strong><small>읽기 전용으로 보관</small>
          </button>
        </div>

        <Card className="invite-quick-card">
          <div>
            <Badge tone="purple">초대받은 모임</Badge>
            <h2>초대 링크가 있으신가요?</h2>
            <p>공유받은 MOIM 초대 URL을 입력하면 참여 화면으로 이동합니다.</p>
          </div>
          <div className="invite-quick-form">
            <input className="input" aria-label="모임 초대 링크" value={inviteUrl} onChange={(event) => setInviteUrl(event.target.value)} />
            <button className="button button-secondary" onClick={openInvite}>이동하기 →</button>
          </div>
          {inviteError && <Message tone="error">{inviteError}</Message>}
        </Card>

        <div className="section-header meeting-list-header">
          <div><h2>{filter === "ALL" ? "전체 모임" : filter === "ACTIVE" ? "진행 중인 모임" : "완료된 모임"}</h2><p>총 {visibleMeetings.length}개</p></div>
        </div>

        <div className="grid grid-3">
          {visibleMeetings.map((meeting) => (
              <MeetingCard
                  key={meeting.id}
                  meeting={meeting}
              />
          ))}
        </div>

        {!error && visibleMeetings.length === 0 && (
            <Card>
              <EmptyState
                  title="진행 중인 모임이 없어요"
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
          {isActive ? "진행 중" : "완료"}
        </Badge>

        <h2>{meeting.name}</h2>

        <p>
          {meeting.description || "등록된 설명이 없습니다."}
        </p>

        <p className="meta"><span>● 참여자 {meeting.participantCount}명</span><span>{isActive ? "지금 참여 가능" : "기록 보관 중"}</span></p>

        <Link
            className="button button-secondary"
            href={`/meetings/${meeting.id}`}
        >
          {isActive ? "상세보기" : "내역보기"} <span aria-hidden="true">→</span>
        </Link>
      </Card>
  );
}
