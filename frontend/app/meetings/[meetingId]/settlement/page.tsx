"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import AppShell from "@/app/_components/AppShell";
import SettlementAccountForm from "@/app/_components/SettlementAccountForm";
import ExpenseReceipt from "@/app/_components/ExpenseReceipt";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, EmptyState, Message, PageTitle, formatDate, formatMoney } from "@/app/_components/ui";
import { apiFetch } from "@/app/_lib/api";
import { getSettlement, type Expense, type ExpenseList } from "@/app/_lib/expenses";
import type { Meeting, MeetingMember, Settlement, SettlementPreview } from "@/app/_lib/types";

type Data = { list: ExpenseList; members: MeetingMember[]; settlement: Settlement | null; meeting: Meeting };

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
  const [refreshing, setRefreshing] = useState(false);
  const refreshRef = useRef(false);
  const [preview, setPreview] = useState<SettlementPreview | null>(null);
  const load = useCallback(async () => {
    const [list, members, settlement, meeting] = await Promise.all([
      apiFetch<ExpenseList>(`/api/meetings/${meetingId}/expenses`),
      apiFetch<MeetingMember[]>(`/api/meetings/${meetingId}/members`),
      getSettlement(meetingId),
      apiFetch<Meeting>(`/api/meetings/${meetingId}`),
    ]);
    return { list, members, settlement, meeting };
  }, [meetingId]);

  useEffect(() => {
    let active = true;
    load().then(result => { if (active) setData(result); })
      .catch(e => { if (active) setError(e instanceof Error ? e.message : "조회하지 못했습니다."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [load]);

  async function refresh() {
    if (refreshRef.current) return;
    refreshRef.current = true; setRefreshing(true); setError(""); setPreview(null);
    try { setData(await load()); }
    catch (e) { setError(e instanceof Error ? e.message : "목록을 다시 불러오지 못했습니다."); }
    finally { refreshRef.current = false; setRefreshing(false); }
  }

  async function previewSettlement() {
    if (busyRef.current || refreshRef.current) return;
    busyRef.current = true; setBusy(true); setError(""); setPreview(null);
    try { setPreview(await apiFetch<SettlementPreview>(`/api/meetings/${meetingId}/settlement/preview`)); }
    catch (e) {
      const message = e instanceof Error ? e.message : "예상 정산을 불러오지 못했습니다.";
      await refresh(); setError(message);
    } finally { busyRef.current = false; setBusy(false); }
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
    if (busyRef.current || refreshRef.current || !preview || !window.confirm("정산을 확정하면 지출과 기존 계좌를 변경할 수 없습니다. 계좌 미등록자가 있어도 확정되며, 미등록 계좌는 나중에 등록할 수 있습니다. 계속할까요?")) return;
    busyRef.current = true; setBusy(true); setError(""); setNotice("");
    try {
      const settlement = await apiFetch<Settlement>(`/api/meetings/${meetingId}/settlement/confirm`, { method: "POST" });
      setData(current => current ? { ...current, settlement, list: { ...current.list, editable: false } } : current);
      setNotice("정산 결과를 확정했습니다. 실제 송금 완료를 의미하지는 않습니다.");
      setPreview(null);
    } catch (e) {
      const message = e instanceof Error ? e.message : "정산 확정에 실패했습니다.";
      await refresh(); setError(message);
    } finally { busyRef.current = false; setBusy(false); }
  }

  const editable = Boolean(data?.list.editable && data.settlement?.status !== "CLOSED");
  const name = (id: number) => data?.members.find(member => member.id === id)?.nickname ?? `모임원 #${id} (기존 참여자)`;
  const settlement = data?.settlement;
  const currentMemberId = data?.list.currentMemberId;
  const myBalance = settlement?.balances.find(balance => balance.memberId === currentMemberId);
  const outgoing = settlement?.transfers.filter(transfer => transfer.senderId === currentMemberId) ?? [];
  const incoming = settlement?.transfers.filter(transfer => transfer.recipientId === currentMemberId) ?? [];

  async function copyAccount(number: string) {
    try { await navigator.clipboard.writeText(number); setNotice("계좌번호를 복사했습니다."); }
    catch { setError("복사하지 못했습니다. 표시된 계좌번호를 직접 복사해주세요."); }
  }

  async function completeMeeting() {
    if (busyRef.current || !window.confirm("모든 활동과 정산 안내를 확인하셨나요? 모임을 종료하면 지출과 계좌를 변경할 수 없습니다.")) return;
    busyRef.current = true; setBusy(true); setError(""); setNotice("");
    try {
      const meeting = await apiFetch<Meeting>(`/api/meetings/${meetingId}/complete`, { method: "PATCH" });
      setData(current => current ? { ...current, meeting, list: { ...current.list, editable: false } } : current);
      setNotice("모임을 종료했습니다.");
    } catch (e) { setError(e instanceof Error ? e.message : "모임 종료에 실패했습니다."); }
    finally { busyRef.current = false; setBusy(false); }
  }
  return <AppShell>
    <PageTitle eyebrow="Expenses & settlement" title="지출 내역 · 정산" description="지출을 확인한 뒤 모임장이 최종 정산을 확정합니다."
      action={!loading && !busy && !refreshing && editable ? <Link className="button button-primary" href={`/meetings/${meetingId}/expenses/new`}>+ 지출 등록</Link> : undefined} />
    <MeetingTabs meetingId={meetingId} active="settlement" />
    {error && <Message tone="error">{error}</Message>}
    {notice && <Message tone="success">{notice}</Message>}
    <button className="button button-ghost" disabled={loading || busy || refreshing} onClick={refresh}>{refreshing ? "갱신 중…" : "새로고침"}</button>
    {loading ? <Message>지출과 정산 정보를 불러오는 중입니다.</Message> : data && <div className="stack">
      <SettlementAccountForm key={meetingId} meetingId={meetingId} closed={settlement?.status === "CLOSED"} meetingOpen={data.meeting.status === "ACTIVE"}
        canRegisterAfterClosed={Boolean(currentMemberId && settlement?.missingAccountMemberIds?.includes(currentMemberId))} onSaved={() => { void refresh(); }} />
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
              <ExpenseReceipt meetingId={meetingId} expenseId={expense.id} hasReceipt={expense.hasReceipt}
                editable={editable && !busy && !refreshing && expense.payerMemberId === currentMemberId} onChange={refresh} />
            </details>
            {editable && expense.payerMemberId === data.list.currentMemberId && <div className="form-actions">
              {!busy && !refreshing && <Link className="button button-secondary" href={`/meetings/${meetingId}/expenses/${expense.id}/edit`}>수정</Link>}
              <button className="button button-ghost" disabled={busy || refreshing} onClick={() => remove(expense)}>삭제</button>
            </div>}
          </div>)}
        </div>}
      </Card>
      {!settlement && data.meeting.status === "ACTIVE" && <Card><Badge tone="green">정산 확정 전</Badge><h2>모든 지출을 확인해주세요</h2>
        <p>예상 결과를 확인한 뒤 확정해주세요. 지출·계좌가 변경되면 결과가 달라질 수 있으며 확정 시 다시 계산합니다.</p>
        <button className="button button-secondary" disabled={busy || refreshing || !editable} onClick={previewSettlement}>{busy ? "처리 중…" : "예상 정산 확인"}</button>
        {preview && <div className="stack">
          <h3>예상 송금 내역</h3>
          {preview.transfers.map(transfer => <p key={`${transfer.senderId}-${transfer.recipientId}`}>{name(transfer.senderId)} → {name(transfer.recipientId)} · {formatMoney(transfer.amount)}</p>)}
          {!preview.transfers.length && <p>추가 송금할 금액이 없습니다.</p>}
          {preview.missingAccounts.length > 0 && <Message>계좌 미등록: {preview.missingAccounts.map(account => account.nickname).join(", ")}. 계좌 등록 여부와 관계없이 정산을 확정할 수 있습니다.</Message>}
        </div>}
        {data.list.leader && editable ? <button className="button button-primary" disabled={busy || refreshing || !preview} onClick={confirmSettlement}>최종 정산 확정</button> : <p>모임장이 정산을 확정하면 결과가 표시됩니다.</p>}
      </Card>}
      {settlement && <>
        {!!settlement.missingAccountMemberIds?.length && <Message>계좌 미등록: {settlement.missingAccountMemberIds.map(name).join(", ")}. 정산은 확정되었으며, 해당 수취인의 계좌 등록 후 송금해주세요.</Message>}
        <Card><Badge tone="gray">정산 확정 완료</Badge><p>{settlement.closedByMemberId && name(settlement.closedByMemberId)} · {formatDate(settlement.closedAt)}</p><p>아래 송금 안내는 실제 송금 완료 기록이 아닙니다.</p></Card>
        <Card><h2>내 정산 결과</h2>
          {myBalance && <p>결제 총액 {formatMoney(myBalance.paidAmount)} · 부담 총액 {formatMoney(myBalance.shareAmount)}</p>}
          <h3>내가 보낼 돈 · {formatMoney(outgoing.reduce((sum, transfer) => sum + transfer.amount, 0))}</h3>
          {outgoing.map(transfer => {
            const account = settlement.accounts?.find(item => item.memberId === transfer.recipientId);
            return <div className="stack" key={transfer.recipientId}>
              <strong>{name(transfer.recipientId)}님에게 {formatMoney(transfer.amount)}</strong>
              {account ? <>
                <p>송금 계좌: {account.bankName} · {account.accountHolder} · {account.accountNumber}</p>
                <button type="button" className="button button-secondary" onClick={() => copyAccount(account.accountNumber)}>계좌번호 복사</button>
              </> : <Message>{name(transfer.recipientId)}님은 계좌 미등록 상태입니다. 계좌 등록 후 송금해주세요.</Message>}
            </div>;
          })}
          <h3>내가 받을 돈 · {formatMoney(incoming.reduce((sum, transfer) => sum + transfer.amount, 0))}</h3>
          {incoming.map(transfer => <p key={transfer.senderId}>{name(transfer.senderId)}님에게 {formatMoney(transfer.amount)} 받기</p>)}
          {!outgoing.length && !incoming.length && <Message>추가로 보내거나 받을 금액이 없습니다.</Message>}
        </Card>
        <Card><h2>개인별 결제·부담 금액</h2><div className="table-wrap"><table><thead><tr><th>모임원</th><th>결제 총액</th><th>부담 총액</th><th>차액 (+받기 / −보내기)</th></tr></thead><tbody>
          {settlement.balances.map(balance => <tr key={balance.memberId}><td>{name(balance.memberId)}</td><td>{formatMoney(balance.paidAmount)}</td><td>{formatMoney(balance.shareAmount)}</td><td>{formatMoney(balance.paidAmount - balance.shareAmount)}</td></tr>)}
        </tbody></table></div></Card>
        <Card><h2>송금 안내</h2>{settlement.transfers.map((transfer, index) => <div className="list-item" key={index}><span>{name(transfer.senderId)} → {name(transfer.recipientId)}</span><strong>{formatMoney(transfer.amount)}</strong></div>)}
          {!settlement.transfers.length && <Message tone="success">추가로 송금할 금액이 없습니다.</Message>}
        </Card>
      </>}
      {data.meeting.status === "COMPLETED" ? <Message>종료된 모임입니다. 지출과 정산 결과를 조회할 수 있습니다.</Message> :
        data.list.leader && (settlement?.status === "CLOSED" || data.list.expenses.length === 0) && <Card>
          <h2>모임 종료</h2><p>모든 활동이 끝났다면 모임을 종료할 수 있습니다. 실제 입금 여부는 별도로 확인해주세요.</p>
          <button className="button button-secondary" disabled={busy || refreshing} onClick={completeMeeting}>모임 종료</button>
        </Card>}
    </div>}
  </AppShell>;
}
