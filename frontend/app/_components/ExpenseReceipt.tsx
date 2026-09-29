"use client";

import { useEffect, useRef, useState } from "react";
import { apiFetch, apiFetchBlob } from "@/app/_lib/api";
import { Message } from "./ui";

export default function ExpenseReceipt({ meetingId, expenseId, hasReceipt, editable, onChange }: {
  meetingId: string; expenseId: number; hasReceipt: boolean; editable: boolean; onChange: () => Promise<void>;
}) {
  const [busy, setBusy] = useState(false);
  const busyRef = useRef(false);
  const [error, setError] = useState("");
  const [url, setUrl] = useState<string | null>(null);
  const live = useRef(true);
  useEffect(() => { live.current = true; return () => { live.current = false; }; }, []);
  useEffect(() => () => { if (url) URL.revokeObjectURL(url); }, [url]);
  const path = `/api/meetings/${meetingId}/expenses/${expenseId}/receipt`;

  async function run(action: () => Promise<void>) {
    if (busyRef.current) return;
    busyRef.current = true; setBusy(true); setError("");
    try { await action(); }
    catch (e) { if (live.current) setError(e instanceof Error ? e.message : "영수증 처리에 실패했습니다."); }
    finally { busyRef.current = false; if (live.current) setBusy(false); }
  }

  async function upload(file: File) {
    if (file.size > 5 * 1024 * 1024 || !["image/jpeg", "image/png"].includes(file.type)) {
      setError("5MB 이하의 JPG 또는 PNG 1장을 선택해주세요."); return;
    }
    await run(async () => {
      const body = new FormData(); body.append("file", file);
      await apiFetch(path, { method: "PUT", body });
      setUrl(null); await onChange();
    });
  }

  return <div className="stack">
    <strong>영수증 {hasReceipt ? "1장 첨부됨" : "미첨부"}</strong>
    {error && <Message tone="error">{error}</Message>}
    {hasReceipt && <button type="button" className="button button-secondary" disabled={busy} onClick={() => run(async () => {
      const blob = await apiFetchBlob(path);
      if (live.current) setUrl(URL.createObjectURL(blob));
    })}>영수증 보기</button>}
    {url && <>
      {/* 인증 요청으로 받은 Blob만 표시합니다. */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={url} alt="지출 영수증" style={{ maxWidth: "100%", maxHeight: 600, objectFit: "contain" }} />
    </>}
    {editable && <>
      <label>영수증 {hasReceipt ? "교체" : "첨부"} (JPG·PNG, 최대 5MB, 1장)
        <input type="file" accept="image/jpeg,image/png" disabled={busy} onChange={e => {
          const file = e.target.files?.[0]; e.target.value = ""; if (file) void upload(file);
        }} />
      </label>
      {hasReceipt && <button type="button" className="button button-ghost" disabled={busy} onClick={() => {
        if (window.confirm("영수증을 삭제할까요?")) void run(async () => {
          await apiFetch(path, { method: "DELETE" }); setUrl(null); await onChange();
        });
      }}>영수증 삭제</button>}
    </>}
  </div>;
}
