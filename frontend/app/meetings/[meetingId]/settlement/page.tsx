"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, EmptyState, Message, PageTitle, formatDate, formatMoney } from "@/app/_components/ui";
import { apiFetch } from "@/app/_lib/api";
import { getSettlement, type Expense, type ExpenseList } from "@/app/_lib/expenses";
import type { MeetingMember, Settlement } from "@/app/_lib/types";

type Data = { list: ExpenseList; members: MeetingMember[]; settlement: Settlement | null };

export default function SettlementPage() {
  const { meetingId } = useParams<{ meetingId: string }>();
  return <SettlementContent key={meetingId} meetingId={meetingId} />;
}

function SettlementContent({ meetingId }: { meetingId: string }) {
  const [data, setData] = useState<Data | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const busyRef = useRef(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const load = useCallback(async () => {
    const [list, members, settlement] = await Promise.all([
      apiFetch<ExpenseList>(`/api/meetings/${meetingId}/expenses`),
      apiFetch<MeetingMember[]>(`/api/meetings/${meetingId}/members`),
      getSettlement(meetingId),
    ]);
    return { list, members, settlement };
  }, [meetingId]);

  useEffect(() => {
    let active = true;
    load().then(result => { if (active) setData(result); })
      .catch(e => { if (active) setError(e instanceof Error ? e.message : "조회하지 못했습니다."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [load]);

  async function refresh() {
    setLoading(true); setError("");
    try { setData(await load()); }
    catch (e) { setData(null); setError(e instanceof Error ? e.message : "목록을 다시 불러오지 못했습니다."); }
    finally { setLoading(false); }
  }

  async function remove(expense: Expense) {
    if (busyRef.current || !window.confirm(`'${expense.title}' 지출을 삭제할까요? 삭제 후에는 복구할 수 없습니다.`)) return;
    busyRef.current = true; setBusy(true); setError(""); setNotice("");
    try {
      await apiFetch(`/api/meetings/${meetingId}/expenses/${expense.id}`, { method: "DELETE" });
      setData(current => current ? { ...current, list: { ...current.list, expenses: current.list.expenses.filter(item => item.id !== expense.id) } } : current);
      setNotice("지출을 삭제했습니다.");
      await refresh();
    } catch (e) {
      const message = e instanceof Error ? e.message : "삭제하지 못했습니다. 목록에서 반영 여부를 확인해주세요.";
      await refresh(); setError(message);
    } finally { busyRef.current = false; setBusy(false); }
  }

  async function confirmSettlement() {
    if (busyRef.current || !window.confirm("정산을 확정하면 지출 등록·수정·삭제가 불가능합니다. 누락된 지출이 없는지 확인하셨나요?")) return;
    busyRef.current = true; setBusy(true); setError(""); setNotice("");
    try {
      const settlement = await apiFetch<Settlement>(`/api/meetings/${meetingId}/settlement/confirm`, { method: "POST" });
      setData(current => current ? { ...current, settlement, list: { ...current.list, editable: false } } : current);
      setNotice("정산 결과를 확정했습니다. 실제 송금 완료를 의미하지는 않습니다.");
    } catch (e) {
      const message = e instanceof Error ? e.message : "정산 확정에 실패했습니다.";
      await refresh(); setError(message);
    } finally { busyRef.current = false; setBusy(false); }
  }

  const editable = Boolean(data?.list.editable && data.settlement?.status !== "CLOSED");
  const name = (id: number) => data?.members.find(member => member.id === id)?.nickname ?? `모임원 #${id} (기존 참여자)`;
  const settlement = data?.settlement;
  return <AppShell>
    <PageTitle eyebrow="Expenses & settlement" title="지출 내역 · 정산" description="지출을 확인한 뒤 모임장이 최종 정산을 확정합니다."
      action={!loading && !busy && editable ? <Link className="button button-primary" href={`/meetings/${meetingId}/expenses/new`}>+ 지출 등록</Link> : undefined} />
    <MeetingTabs meetingId={meetingId} active="settlement" />
    {error && <Message tone="error">{error}</Message>}
    {notice && <Message tone="success">{notice}</Message>}
    <button className="button button-ghost" disabled={loading || busy} onClick={refresh}>새로고침</button>
    {loading ? <Message>지출과 정산 정보를 불러오는 중입니다.</Message> : data && <div className="stack">
      <Card>
        <div className="row between"><h2>지출 {data.list.expenses.length}건</h2><strong>합계 {formatMoney(data.list.expenses.reduce((sum, expense) => sum + expense.amount, 0))}</strong></div>
        {!editable && <Message>정산이 확정되었거나 종료된 모임입니다. 지출은 조회만 가능합니다.</Message>}
        <p className="muted">확정 전에는 본인이 등록한 지출만 수정·삭제할 수 있습니다.</p>
        {data.list.expenses.length === 0 ? <EmptyState title="등록된 지출이 없습니다" description="결제한 내역을 등록하면 이곳에서 확인할 수 있습니다." /> : <div className="list">
          {data.list.expenses.map(expense => <div className="stack" key={expense.id}>
            <div className="list-item"><div><strong>{expense.title}</strong><p>{name(expense.payerMemberId)} 결제 · {formatDate(expense.createdAt)}</p></div><strong>{formatMoney(expense.amount)}</strong></div>
            <details><summary>상세 내역</summary>
              <p>{expense.memo || "등록된 메모가 없습니다."}</p>
              <p>분담 방식: {expense.splitMode === "EXACT" ? "금액 직접 입력" : "균등 분담"}</p>
              {expense.participants.map(share => <p key={share.memberId}>{name(share.memberId)}: {formatMoney(share.amount)}</p>)}
              {!expense.participants.length && <p>이전 지출의 개인별 부담액 정보가 없습니다.</p>}
              {expense.remainderMemberId && <p>차액 담당자: {name(expense.remainderMemberId)}</p>}
            </details>
            {editable && expense.payerMemberId === data.list.currentMemberId && <div className="form-actions">
              {!busy && <Link className="button button-secondary" href={`/meetings/${meetingId}/expenses/${expense.id}/edit`}>수정</Link>}
              <button className="button button-ghost" disabled={busy} onClick={() => remove(expense)}>삭제</button>
            </div>}
          </div>)}
        </div>}
      </Card>
      {!settlement && <Card><Badge tone="green">정산 확정 전</Badge><h2>모든 지출을 확인해주세요</h2>
        {data.list.leader && editable ? <button className="button button-primary" disabled={busy} onClick={confirmSettlement}>{busy ? "처리 중…" : "최종 정산 확정"}</button> : <p>모임장이 정산을 확정하면 결과가 표시됩니다.</p>}
      </Card>}
      {settlement && <>
        <Card><Badge tone="gray">정산 확정 완료</Badge><p>{settlement.closedByMemberId && name(settlement.closedByMemberId)} · {formatDate(settlement.closedAt)}</p><p>아래 송금 안내는 실제 송금 완료 기록이 아닙니다.</p></Card>
        <Card><h2>개인별 결제·부담 금액</h2><div className="table-wrap"><table><thead><tr><th>모임원</th><th>결제 총액</th><th>부담 총액</th><th>차액 (+받기 / −보내기)</th></tr></thead><tbody>
          {settlement.balances.map(balance => <tr key={balance.memberId}><td>{name(balance.memberId)}</td><td>{formatMoney(balance.paidAmount)}</td><td>{formatMoney(balance.shareAmount)}</td><td>{formatMoney(balance.paidAmount - balance.shareAmount)}</td></tr>)}
        </tbody></table></div></Card>
        <Card><h2>송금 안내</h2>{settlement.transfers.map((transfer, index) => <div className="list-item" key={index}><span>{name(transfer.senderId)} → {name(transfer.recipientId)}</span><strong>{formatMoney(transfer.amount)}</strong></div>)}
          {!settlement.transfers.length && <Message tone="success">추가로 송금할 금액이 없습니다.</Message>}
        </Card>
      </>}
    </div>}
  </AppShell>;
}