"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Card, Field, Message, PageTitle } from "@/app/_components/ui";
import { apiFetch, jsonBody } from "@/app/_lib/api";

export default function ContentCandidateCreatePage() {
  const { meetingId } = useParams<{ meetingId: string }>();
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;
    setSaving(true);
    setError("");
    try {
      await apiFetch(`/api/meetings/${meetingId}/content-poll/candidates`, {
        method: "POST",
        body: jsonBody({ title: title.trim(), description: description.trim() || null }),
      });
      router.push(`/meetings/${meetingId}/content`);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "후보를 등록하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  }

  return <AppShell>
    <div className="breadcrumb"><Link href={`/meetings/${meetingId}/content`}>← 콘텐츠 투표</Link><span>/</span><span>후보 제안</span></div>
    <PageTitle eyebrow="New content idea" title="새로운 콘텐츠 후보 등록" description="모임원들과 함께하고 싶은 장소나 활동을 제안해 보세요." />
    <MeetingTabs meetingId={meetingId} active="content" />
    <Card className="candidate-form-card">
      <form className="stack" onSubmit={submit}>
        <Field label="콘텐츠 이름" hint={`${title.length} / 100`}>
          <input className="input" maxLength={100} required placeholder="예: 성산일출봉 해돋이 트레킹" value={title} onChange={(event) => setTitle(event.target.value)} />
        </Field>
        <Field label="상세 설명 및 제안 메모 (선택)" hint={`${description.length} / 500 · 일반 텍스트로 저장됩니다.`}>
          <textarea className="textarea candidate-description" maxLength={500} placeholder="장소, 준비물, 예상 소요 시간처럼 함께 알아야 할 내용을 적어주세요." value={description} onChange={(event) => setDescription(event.target.value)} />
        </Field>
        {error && <Message tone="error">{error}</Message>}
        <div className="form-actions"><Link className="button button-ghost" href={`/meetings/${meetingId}/content`}>취소</Link><button className="button button-primary" disabled={saving || !title.trim()}>{saving ? "등록 중…" : "＋ 후보 등록 완료"}</button></div>
      </form>
    </Card>
  </AppShell>;
}
