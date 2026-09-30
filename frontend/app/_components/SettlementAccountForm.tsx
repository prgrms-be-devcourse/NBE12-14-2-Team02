"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { apiFetch, ApiError, jsonBody } from "@/app/_lib/api";
import type { SettlementAccount } from "@/app/_lib/types";
import { Badge, Card, Field, Message } from "./ui";

export default function SettlementAccountForm({ meetingId, closed, meetingOpen, canRegisterAfterClosed = false, onSaved }: { meetingId: string; closed: boolean; meetingOpen: boolean; canRegisterAfterClosed?: boolean; onSaved?: () => void }) {
  const [bankName, setBankName] = useState("");
  const [accountNumber, setAccountNumber] = useState("");
  const [accountHolder, setAccountHolder] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const busyRef = useRef(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [account, setAccount] = useState<SettlementAccount | null>(null);
  const [editing, setEditing] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [locked, setLocked] = useState(false);
  const [loadAttempt, setLoadAttempt] = useState(0);
  const [registeredAfterClosed, setRegisteredAfterClosed] = useState(false);
  const editable = meetingOpen && (!closed || (canRegisterAfterClosed && !registeredAfterClosed)) && !locked;
  useEffect(() => {
    let active = true;
    apiFetch<SettlementAccount | null>(`/api/meetings/${meetingId}/settlement/account`)
      .then(saved => {
        if (active) { setAccount(saved); setLoaded(true); }
      }).catch(e => { if (active) setError(e instanceof Error ? e.message : "계좌 조회에 실패했습니다."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [meetingId, loadAttempt]);

  function startEditing() {
    setBankName(account?.bankName ?? "");
    setAccountNumber(account?.accountNumber ?? "");
    setAccountHolder(account?.accountHolder ?? "");
    setError(""); setNotice(""); setEditing(true);
  }

  const digits = account?.accountNumber.replace(/\D/g, "") ?? "";
  const maskedNumber = digits.length > 4 ? `${"•".repeat(digits.length - 4)}${digits.slice(-4)}` : "•".repeat(digits.length);

  async function save(event: FormEvent) {
    event.preventDefault();
    if (busyRef.current || !editable || !loaded || !editing) return;
    if (!bankName.trim() || !accountHolder.trim()) {
      setError("은행명과 예금주를 입력해주세요."); return;
    }
    busyRef.current = true; setBusy(true); setError(""); setNotice("");
    try {
      const saved = await apiFetch<SettlementAccount>(`/api/meetings/${meetingId}/settlement/account`, {
        method: "PUT", body: jsonBody({ bankName: bankName.trim(), accountNumber: accountNumber.trim(), accountHolder: accountHolder.trim() }),
      });
      setAccount(saved); setEditing(false);
      if (closed) setRegisteredAfterClosed(true);
      onSaved?.();
      setNotice("정산 계좌를 저장했습니다.");
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) { setLocked(true); setEditing(false); }
      setError(e instanceof Error ? e.message : "계좌 저장에 실패했습니다.");
    }
    finally { busyRef.current = false; setBusy(false); }
  }

  return <Card className="account-card"><div className="row between"><div><Badge tone="green">수취 계좌</Badge><h2>내 정산 계좌</h2></div><span className="feature-icon" aria-hidden="true">₩</span></div>
    {closed ? <p className="muted">{editable ? "정산은 확정되었습니다. 정산금을 받을 계좌를 등록해주세요. 등록한 계좌는 송금 안내에 반영되며 이후 변경할 수 없습니다." : "정산이 확정되어 기존 계좌는 변경할 수 없습니다. 송금할 계좌는 아래 정산 결과에서 확인해주세요."}</p>
      : !meetingOpen && <p className="muted">종료된 모임의 계좌는 조회만 가능합니다.</p>}
    {error && <Message tone="error">{error}</Message>}{notice && <Message tone="success">{notice}</Message>}
    {loading ? <p className="muted">계좌 정보를 불러오는 중입니다.</p> : !loaded ?
      <button type="button" className="button button-secondary" onClick={() => { setError(""); setLoading(true); setLoadAttempt(value => value + 1); }}>다시 불러오기</button>
      : editing && editable ? <form className="stack" onSubmit={save}>
      <fieldset disabled={busy} className="grid grid-3" style={{ border: 0, padding: 0, minWidth: 0 }}>
        <Field label="은행명"><input className="input" required maxLength={100} value={bankName} onChange={e => setBankName(e.target.value)} /></Field>
        <Field label="계좌번호"><input className="input" type="text" inputMode="numeric" required maxLength={50} pattern="[0-9]+(-[0-9]+)*" value={accountNumber} onChange={e => setAccountNumber(e.target.value)} /></Field>
        <Field label="예금주"><input className="input" required maxLength={100} value={accountHolder} onChange={e => setAccountHolder(e.target.value)} /></Field>
      </fieldset>
      <p className="muted">계좌번호와 예금주를 정확히 확인해주세요. 실제 계좌의 존재 여부는 자동 확인되지 않습니다.</p>
      <div className="form-actions">
        <button type="button" className="button button-ghost" disabled={busy} onClick={() => { setEditing(false); setError(""); }}>취소</button>
        <button className="button button-primary" disabled={busy}>{busy ? "저장 중…" : "계좌 저장"}</button>
      </div>
    </form> : <div className="row between">
      {account ? <div><p><strong>{account.bankName}</strong> · {account.accountHolder}</p><p className="muted">{maskedNumber}</p></div>
        : <p>{editable ? "이 모임에서 정산금을 받을 계좌를 등록해주세요." : "등록된 정산 계좌가 없습니다."}</p>}
      {editable && <button type="button" className="button button-secondary" onClick={startEditing}>{account ? "수정" : "계좌 등록"}</button>}
    </div>}
  </Card>;
}
