"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { apiFetch, jsonBody } from "@/app/_lib/api";
import type { SettlementAccount } from "@/app/_lib/types";
import { Card, Field, Message } from "./ui";

export default function SettlementAccountForm({ meetingId, closed, meetingOpen }: { meetingId: string; closed: boolean; meetingOpen: boolean }) {
  const [bankName, setBankName] = useState("");
  const [accountNumber, setAccountNumber] = useState("");
  const [accountHolder, setAccountHolder] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const busyRef = useRef(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  useEffect(() => {
    let active = true;
    apiFetch<SettlementAccount | null>(`/api/meetings/${meetingId}/settlement/account`)
      .then(account => {
        if (active && account) {
          setBankName(account.bankName); setAccountNumber(account.accountNumber); setAccountHolder(account.accountHolder);
        }
      }).catch(e => { if (active) setError(e instanceof Error ? e.message : "계좌 조회에 실패했습니다."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [meetingId]);

  async function save(event: FormEvent) {
    event.preventDefault();
    if (busyRef.current) return;
    busyRef.current = true; setBusy(true); setError(""); setNotice("");
    try {
      await apiFetch(`/api/meetings/${meetingId}/settlement/account`, {
        method: "PUT", body: jsonBody({ bankName: bankName.trim(), accountNumber: accountNumber.trim(), accountHolder: accountHolder.trim() }),
      });
      setNotice("계좌를 저장했습니다. 모임장이 다시 정산을 확정할 수 있습니다.");
    } catch (e) { setError(e instanceof Error ? e.message : "계좌 저장에 실패했습니다."); }
    finally { busyRef.current = false; setBusy(false); }
  }

  return <Card><h2>내 정산 계좌</h2>
    <p>이 모임에서 돈을 받을 계좌를 등록해주세요. 실제 수취인의 계좌가 모두 등록되어야 정산을 확정할 수 있습니다.</p>
    {closed && <Message>정산이 확정되었습니다. 여기서 계좌를 수정해도 확정 당시 송금 안내의 계좌는 바뀌지 않습니다. 종료된 모임에서는 수정할 수 없습니다.</Message>}
    {error && <Message tone="error">{error}</Message>}{notice && <Message tone="success">{notice}</Message>}
    <form className="stack" onSubmit={save}>
      <fieldset disabled={busy || loading || !meetingOpen} className="grid grid-3" style={{ border: 0, padding: 0, minWidth: 0 }}>
        <Field label="은행명"><input className="input" required maxLength={100} value={bankName} onChange={e => setBankName(e.target.value)} /></Field>
        <Field label="계좌번호"><input className="input" type="text" inputMode="numeric" required maxLength={50} pattern="[0-9]+(-[0-9]+)*" value={accountNumber} onChange={e => setAccountNumber(e.target.value)} /></Field>
        <Field label="예금주"><input className="input" required maxLength={100} value={accountHolder} onChange={e => setAccountHolder(e.target.value)} /></Field>
      </fieldset>
      <p className="muted">계좌번호와 예금주를 정확히 확인해주세요. 실제 계좌의 존재 여부는 자동 확인되지 않습니다.</p>
      <button className="button button-primary" disabled={busy || loading || !meetingOpen}>{busy ? "저장 중…" : "내 계좌 저장"}</button>
    </form>
  </Card>;
}
