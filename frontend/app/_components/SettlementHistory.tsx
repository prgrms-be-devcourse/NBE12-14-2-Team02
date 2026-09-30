"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/app/_lib/api";
import type { SettlementHistoryEntry } from "@/app/_lib/types";
import { Card, Message, formatDate, formatMoney } from "./ui";

export default function SettlementHistory() {
  const [entries, setEntries] = useState<SettlementHistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    apiFetch<SettlementHistoryEntry[]>("/api/settlements/mine")
      .then(result => { if (active) setEntries(result); })
      .catch(e => { if (active) setError(e instanceof Error ? e.message : "정산 내역을 불러오지 못했습니다."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [attempt]);

  async function copy(number: string) {
    setNotice("");
    try { await navigator.clipboard.writeText(number); setNotice("계좌번호를 복사했습니다."); }
    catch { setNotice("복사하지 못했습니다. 표시된 계좌번호를 직접 복사해주세요."); }
  }

  return <Card>
    <h2>내 정산 내역</h2>
    <p>탈퇴한 모임을 포함해 본인의 확정된 송금 안내를 확인할 수 있습니다. 실제 송금 완료 여부는 별도로 확인해주세요.</p>
    {notice && <Message>{notice}</Message>}
    {error && <Message tone="error">{error}</Message>}
    <button type="button" className="button button-ghost" disabled={loading} onClick={() => { setError(""); setLoading(true); setAttempt(value => value + 1); }}>다시 불러오기</button>
    {loading ? <p>정산 내역을 불러오는 중입니다.</p> : !error && !entries.length ? <p>확정된 정산 내역이 없습니다.</p> : entries.map(entry => {
      const { result, currentMemberId, names } = entry;
      const outgoing = result.transfers.filter(transfer => transfer.senderId === currentMemberId);
      const incoming = result.transfers.filter(transfer => transfer.recipientId === currentMemberId);
      const balance = result.balances.find(item => item.memberId === currentMemberId);
      const name = (id: number) => names[String(id)] ?? `모임원 #${id}`;
      return <details key={result.settlementId}>
        <summary>{entry.meetingName} · {formatDate(result.closedAt)}</summary>
        {!!result.missingAccountMemberIds?.length && <Message>계좌 미등록: {result.missingAccountMemberIds.map(name).join(", ")}</Message>}
        {balance && <p>결제 총액 {formatMoney(balance.paidAmount)} · 부담 총액 {formatMoney(balance.shareAmount)}</p>}
        <h3>내가 보낼 돈</h3>
        {outgoing.map(transfer => {
          const account = result.accounts.find(item => item.memberId === transfer.recipientId);
          return <div className="stack" key={transfer.recipientId}>
            <p>{name(transfer.recipientId)}님에게 {formatMoney(transfer.amount)}</p>
            {account ? <><p>{account.bankName} · {account.accountHolder} · {account.accountNumber}</p><button type="button" className="button button-secondary" onClick={() => copy(account.accountNumber)}>계좌번호 복사</button></> : <Message>{name(transfer.recipientId)}님은 계좌 미등록 상태입니다. 계좌 등록 후 송금해주세요.</Message>}
          </div>;
        })}
        {!outgoing.length && <p>보낼 돈이 없습니다.</p>}
        <h3>내가 받을 돈</h3>
        {incoming.map(transfer => <p key={transfer.senderId}>{name(transfer.senderId)}님에게 {formatMoney(transfer.amount)} 받기</p>)}
        {!incoming.length && <p>받을 돈이 없습니다.</p>}
      </details>;
    })}
  </Card>;
}
