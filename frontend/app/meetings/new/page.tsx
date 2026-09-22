"use client";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import AppShell from "@/app/_components/AppShell";
import { Card, Field, Message, PageTitle } from "@/app/_components/ui";
import { apiFetch, jsonBody } from "@/app/_lib/api";
import type { Meeting } from "@/app/_lib/types";

export default function MeetingCreatePage() {
  const router = useRouter(); const [error, setError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const values = new FormData(event.currentTarget); try { const meeting = await apiFetch<Meeting>("/api/meetings", { method: "POST", body: jsonBody({ name: values.get("name"), description: values.get("description") }) }); router.push(`/meetings/${meeting.id}`); } catch (e) { setError(e instanceof Error ? e.message : "모임을 만들지 못했습니다."); } }
  return <AppShell><PageTitle eyebrow="New meeting" title="새 모임 만들기" description="기본 정보를 입력한 뒤 일정과 콘텐츠 후보를 추가할 수 있어요." /><Card><form className="stack" onSubmit={submit}><Field label="모임 이름"><input className="input" name="name" required maxLength={100} placeholder="예: 제주도 워케이션" /></Field><Field label="설명 (선택)"><textarea className="textarea" name="description" placeholder="모임의 목적과 간단한 안내를 적어주세요." /></Field>{error && <Message tone="error">{error}</Message>}<div className="form-actions"><button type="button" className="button button-ghost" onClick={() => router.back()}>취소</button><button className="button button-primary">모임 생성하기</button></div></form></Card></AppShell>;
}
