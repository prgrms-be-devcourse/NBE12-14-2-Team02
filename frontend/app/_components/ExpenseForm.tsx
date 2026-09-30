"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, type FormEvent } from "react";
import AppShell from "./AppShell";
import MeetingTabs from "./MeetingTabs";
import { Badge, Card, Field, Message, PageTitle, formatMoney } from "./ui";
import { apiFetch, ApiError, jsonBody } from "@/app/_lib/api";
import type { MeetingMember } from "@/app/_lib/types";
import type { Expense, ExpenseList } from "@/app/_lib/expenses";

export default function ExpenseForm({ meetingId, expenseId }: { meetingId: string; expenseId?: string }) {
  const router = useRouter();
  const [members, setMembers] = useState<Array<{ id: number; nickname: string }>>([]);
  const [title, setTitle] = useState("");
  const [amount, setAmount] = useState("");
  const [memo, setMemo] = useState("");
  const [selected, setSelected] = useState<number[]>([]);
  const [amounts, setAmounts] = useState<Record<number, string>>({});
  const [mode, setMode] = useState<"EQUAL" | "EXACT">("EQUAL");
  const [unit, setUnit] = useState(1);
  const [remainderId, setRemainderId] = useState("");
  const [random, setRandom] = useState(false);
  const [payer, setPayer] = useState("");
  const [loading, setLoading] = useState(true);
  const [allowed, setAllowed] = useState(false);
  const [saving, setSaving] = useState(false);
  const savingRef = useRef(false);
  const [error, setError] = useState("");
  const [receipt, setReceipt] = useState<File | null>(null);
  const receiptInput = useRef<HTMLInputElement>(null);
  const [submitted, setSubmitted] = useState(false);
  const [savedId, setSavedId] = useState<number | null>(null);
  const pending = useRef<{ key: string; body: string; savedId?: number } | null>(null);
  const storageKey = useRef("");

  useEffect(() => {
    let active = true;
    Promise.all([
      apiFetch<MeetingMember[]>(`/api/meetings/${meetingId}/members`),
      apiFetch<ExpenseList>(`/api/meetings/${meetingId}/expenses`),
      expenseId ? apiFetch<Expense>(`/api/meetings/${meetingId}/expenses/${expenseId}`) : Promise.resolve(null),
    ]).then(([joined, list, expense]) => {
      if (!active) return;
      const options = joined.map(({ id, nickname }) => ({ id, nickname }));
      // 탈퇴한 기존 부담자를 편집 과정에서 조용히 누락하지 않습니다.
      for (const share of expense?.participants ?? []) {
        if (!options.some(member => member.id === share.memberId)) {
          options.push({ id: share.memberId, nickname: `모임원 #${share.memberId} (기존 참여자)` });
        }
      }
      setMembers(options);
      const payerId = expense?.payerMemberId ?? list.currentMemberId;
      setPayer(options.find(member => member.id === payerId)?.nickname ?? `모임원 #${payerId}`);
      if (!list.editable) throw new Error("정산이 확정되었거나 종료된 모임입니다. 지출을 변경할 수 없습니다.");
      if (expense && expense.payerMemberId !== list.currentMemberId) throw new Error("본인이 등록한 지출만 수정할 수 있습니다.");
      setAllowed(true);
      if (expense) {
        if (expense.participants.length === 0) {
          setAllowed(false);
          throw new Error("이전 지출에 개인별 부담액 정보가 없어 안전하게 편집할 수 없습니다.");
        }
        setTitle(expense.title); setAmount(String(expense.amount)); setMemo(expense.memo ?? "");
        setMode(expense.splitMode ?? "EXACT"); setUnit(expense.roundingUnit ?? 1);
        setRemainderId(expense.remainderMemberId ? String(expense.remainderMemberId) : "");
        setSelected(expense.participants.map(share => share.memberId));
        setAmounts(Object.fromEntries(expense.participants.map(share => [share.memberId, String(share.amount)])));
      } else {
        setSelected(joined.map(member => member.id));
        storageKey.current = `expense-create:${meetingId}:${list.currentMemberId}`;
        const stored = sessionStorage.getItem(storageKey.current);
        if (stored) {
          const previous = JSON.parse(stored);
          if (typeof previous.key === "string" && typeof previous.body === "string") {
            const draft = JSON.parse(previous.body);
            pending.current = previous; setSubmitted(true); setSavedId(previous.savedId ?? null);
            setTitle(draft.title); setAmount(String(draft.amount)); setMemo(draft.memo ?? "");
            setMode(draft.splitMode); setUnit(draft.roundingUnit ?? 1); setRandom(draft.randomRemainder);
            setRemainderId(draft.remainderMemberId ? String(draft.remainderMemberId) : "");
            setSelected(draft.participants.map((p: { memberId: number }) => p.memberId));
            setAmounts(Object.fromEntries(draft.participants.map((p: { memberId: number; amount: number | null }) => [p.memberId, p.amount === null ? "" : String(p.amount)])));
          }
        }
      }
    }).catch(e => { if (active) setError(e instanceof Error ? e.message : "화면을 불러오지 못했습니다."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [meetingId, expenseId]);

  const total = Number(amount);
  const each = selected.length ? Math.floor(total / selected.length / unit) * unit : 0;
  const remainder = total - each * selected.length;
  const exactTotal = selected.reduce((sum, id) => sum + Number(amounts[id] || 0), 0);

  function toggle(id: number) {
    setSelected(items => items.includes(id) ? items.filter(item => item !== id) : [...items, id]);
    if (remainderId === String(id)) setRemainderId("");
  }

  function chooseReceipt(file?: File) {
    setError("");
    if (file && (file.size === 0 || file.size > 5 * 1024 * 1024 || !["image/jpeg", "image/png"].includes(file.type))) {
      setReceipt(null);
      if (receiptInput.current) receiptInput.current.value = "";
      setError("5MB 이하의 JPG 또는 PNG 1장을 선택해주세요."); return;
    }
    setReceipt(file ?? null);
  }

  async function saveRegistration() {
    const attempt = pending.current;
    if (!attempt) return;
    if (!attempt.savedId) {
      const saved = await apiFetch<Expense>(`/api/meetings/${meetingId}/expenses`, {
        method: "POST", headers: { "Idempotency-Key": attempt.key }, body: attempt.body,
      });
      attempt.savedId = saved.id; setSavedId(saved.id);
      sessionStorage.setItem(storageKey.current, JSON.stringify(attempt));
    }
    if (receipt) {
      const body = new FormData(); body.append("file", receipt);
      await apiFetch(`/api/meetings/${meetingId}/expenses/${attempt.savedId}/receipt`, { method: "PUT", body });
    }
    sessionStorage.removeItem(storageKey.current);
    router.push(`/meetings/${meetingId}/settlement`);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (savingRef.current || !allowed) return;
    setError("");
    if (!pending.current && (!title.trim() || !Number.isSafeInteger(total) || total <= 0 || total > 999999999999)) {
      setError("제목과 1원 이상의 정수 금액을 입력해주세요."); return;
    }
    if (selected.length === 0 || selected.length > 100) { setError("부담 참여자를 1~100명 선택해주세요."); return; }
    if (!pending.current && mode === "EXACT" && (exactTotal !== total || selected.some(id => amounts[id] === undefined || amounts[id] === "" || !Number.isSafeInteger(Number(amounts[id])) || Number(amounts[id]) < 0))) {
      setError("모든 참여자의 부담액을 입력하고 합계를 결제 금액에 맞춰주세요."); return;
    }
    if (mode === "EQUAL" && remainder > 0 && !random && !selected.includes(Number(remainderId))) {
      setError("남은 차액을 부담할 참여자를 선택해주세요."); return;
    }
    savingRef.current = true; setSaving(true);
    try {
      const body = jsonBody({ title: title.trim(), amount: total, memo, splitMode: mode,
          participants: selected.map(memberId => ({ memberId, amount: mode === "EXACT" ? Number(amounts[memberId]) : null })),
          roundingUnit: mode === "EQUAL" ? unit : null,
          remainderMemberId: mode === "EQUAL" && remainder > 0 && !random ? Number(remainderId) : null,
          randomRemainder: mode === "EQUAL" && remainder > 0 && random });
      if (expenseId) {
        await apiFetch(`/api/meetings/${meetingId}/expenses/${expenseId}`, { method: "PUT", body });
        router.push(`/meetings/${meetingId}/settlement`);
      } else {
        if (!pending.current) {
          const attempt = { key: crypto.randomUUID(), body };
          sessionStorage.setItem(storageKey.current, JSON.stringify(attempt));
          pending.current = attempt; setSubmitted(true);
        }
        await saveRegistration();
      }
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) setAllowed(false);
      if (e instanceof ApiError && ((e.status === 400 && !pending.current?.savedId) || e.status === 410)) {
        pending.current = null; setSubmitted(false);
        setSavedId(null);
        sessionStorage.removeItem(storageKey.current);
      }
      const message = e instanceof Error ? e.message : "저장 응답을 확인하지 못했습니다.";
      setError(pending.current?.savedId ? `지출은 저장되었습니다. 영수증만 다시 시도하거나 목록에서 확인해주세요. ${message}` : message);
    } finally { savingRef.current = false; setSaving(false); }
  }

  return <AppShell>
    <div className="breadcrumb"><Link href={`/meetings/${meetingId}/settlement`}>← 지출 목록</Link><span>/</span><span>{expenseId ? "지출 수정" : "새 지출"}</span></div>
    <PageTitle eyebrow="Expense details" title={expenseId ? "지출 내역 수정" : "새 지출 내역 등록"} description="결제 정보와 부담 대상자를 확인한 뒤 저장해 주세요." />
    <MeetingTabs meetingId={meetingId} active="settlement" />
    {error && <Message tone="error">{error}</Message>}
    {loading ? <Message>지출 정보를 불러오는 중입니다.</Message> : <Card className="expense-form-card">
      <form className="stack" onSubmit={submit}>
        {submitted && <Message>{savedId ? "지출은 저장되었습니다. 영수증만 다시 저장할 수 있습니다." : "이전에 보낸 등록 요청을 확인 중입니다. 다시 저장해도 같은 요청 번호로 처리됩니다."} 새로고침 후에는 영수증 사진을 다시 선택해주세요.</Message>}
        <fieldset disabled={!allowed || saving || submitted} className="stack" style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}>
          <div className="form-section-heading"><span>1</span><div><h2>기본 정보</h2><p>결제한 지출의 이름과 금액을 입력해 주세요.</p></div></div>
          <div className="grid grid-2">
            <Field label="지출 제목"><input className="input" required maxLength={255} value={title} onChange={e => setTitle(e.target.value)} /></Field>
            <Field label="결제 금액 (원)"><input className="input" type="number" required min={1} max={999999999999} step={1} value={amount} onChange={e => setAmount(e.target.value)} /></Field>
          </div>
          <Field label="메모"><textarea className="textarea" maxLength={2000} value={memo} onChange={e => setMemo(e.target.value)} /></Field>
          <div className="payer-panel"><span className="member-avatar">{payer.charAt(0)}</span><div><small>결제자 · 현재 로그인 사용자</small><strong>{payer}</strong></div><Badge tone="purple">본인 지출</Badge></div>
          <div className="form-section-heading"><span>2</span><div><h2>부담 참여자</h2><p>이 지출을 함께 부담할 모임원을 선택해 주세요.</p></div></div>
          <Field label="분담 방식"><select className="select" value={mode} onChange={e => setMode(e.target.value as "EQUAL" | "EXACT")}><option value="EQUAL">균등 분담</option><option value="EXACT">금액 직접 입력</option></select></Field>
          <div className="participant-list"><div className="row between"><strong>참여자 선택</strong><span className="muted">{selected.length}명 선택</span></div>{members.map(member => <div className="checkbox-row" key={member.id}>
            <label><input type="checkbox" checked={selected.includes(member.id)} onChange={() => toggle(member.id)} /> {member.nickname}</label>
            {mode === "EXACT" && selected.includes(member.id) && <input aria-label={`${member.nickname} 부담액`} className="input" type="number" min={0} max={999999999999} step={1} required value={amounts[member.id] ?? ""} onChange={e => setAmounts(all => ({ ...all, [member.id]: e.target.value }))} />}
          </div>)}</div>
          <div className="form-section-heading"><span>3</span><div><h2>분담 결과</h2><p>저장 전 개인별 부담 금액을 확인해 주세요.</p></div></div>
          {mode === "EQUAL" ? <>
            <Field label="분담 단위"><select className="select" value={unit} onChange={e => setUnit(Number(e.target.value))}>{[1, 10, 100, 1000].map(value => <option value={value} key={value}>{value}원</option>)}</select></Field>
            <Message>기본 부담액 {formatMoney(each)} · 남은 차액 {formatMoney(remainder)}</Message>
            {remainder > 0 && <>
              <label className="checkbox-row"><input type="checkbox" checked={random} onChange={e => setRandom(e.target.checked)} />차액 담당자 무작위 선택 (저장할 때 결정)</label>
              {!random && <Field label="차액 담당자"><select className="select" value={remainderId} onChange={e => setRemainderId(e.target.value)} required><option value="">선택해주세요</option>{members.filter(member => selected.includes(member.id)).map(member => <option value={member.id} key={member.id}>{member.nickname}</option>)}</select></Field>}
            </>}
          </> : <Message>입력 합계 {formatMoney(exactTotal)} · 남은 금액 {formatMoney(total - exactTotal)}</Message>}
        </fieldset>
        {!expenseId ? <>
          <Field label="영수증 사진 (선택)"><input ref={receiptInput} type="file" accept="image/jpeg,image/png" disabled={!allowed || saving} onChange={e => chooseReceipt(e.target.files?.[0])} /></Field>
          <p className="muted">JPG·PNG 최대 5MB, 1장. 저장 후 상세 내역에서 교체·삭제할 수 있습니다.</p>
          {receipt && <div className="row"><span>{receipt.name}</span><button type="button" className="button button-ghost" disabled={saving} onClick={() => { setReceipt(null); if (receiptInput.current) receiptInput.current.value = ""; }}>선택 해제</button></div>}
        </> : <p className="muted">영수증 교체·삭제는 지출 목록의 상세 내역에서 할 수 있습니다.</p>}
        <div className="form-actions"><Link href={`/meetings/${meetingId}/settlement`} className="button button-ghost">지출 목록</Link><button className="button button-primary" disabled={!allowed || saving || !selected.length}>{saving ? "저장 중…" : expenseId ? "수정 저장" : savedId ? receipt ? "영수증만 저장" : "현재 상태로 완료" : submitted ? "같은 요청 다시 확인" : "지출 등록 완료"}</button></div>
      </form>
    </Card>}
  </AppShell>;
}
