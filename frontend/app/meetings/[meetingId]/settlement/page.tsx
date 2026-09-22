"use client";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import AppShell from "@/app/_components/AppShell";
import MeetingTabs from "@/app/_components/MeetingTabs";
import { Badge, Card, Message, PageTitle, formatDate, formatMoney } from "@/app/_components/ui";
import { apiFetch } from "@/app/_lib/api";
import type { Settlement } from "@/app/_lib/types";

export default function SettlementPage() {
  const { meetingId } = useParams<{ meetingId: string }>(); const [settlement, setSettlement] = useState<Settlement | null>(null); const [error, setError] = useState("");
  useEffect(() => { apiFetch<Settlement>(`/api/meetings/${meetingId}/settlement`).then(setSettlement).catch(() => setSettlement(null)); }, [meetingId]);
  async function confirm() { try { const result = await apiFetch<Settlement>(`/api/meetings/${meetingId}/settlement/confirm`, { method: "POST" }); setSettlement(result); } catch (e) { setError(e instanceof Error ? e.message : "정산을 확정하지 못했습니다."); } }
  return <AppShell><PageTitle eyebrow="Settlement" title="최종 정산" description="지출 등록을 마친 뒤 호스트가 최종 송금 관계를 확정합니다." action={<Link className="button button-secondary" href={`/meetings/${meetingId}/expenses/new`}>+ 지출 등록</Link>} /><MeetingTabs meetingId={meetingId} active="settlement" />{error && <Message tone="error">{error}</Message>}{!settlement && <Card className="stack"><div className="row between"><div><Badge tone="green">OPEN</Badge><h2>정산을 확정할 준비가 되었나요?</h2><p>모든 지출 등록을 확인한 뒤 호스트가 확정할 수 있습니다.</p></div><button className="button button-primary" onClick={confirm}>최종 정산 확정</button></div></Card>}{settlement && <div className="stack"><Card><div className="row between"><div><Badge tone={settlement.status === "CLOSED" ? "gray" : "green"}>{settlement.status}</Badge><h2>정산 #{settlement.settlementId}</h2></div>{settlement.status === "OPEN" && <button className="button button-primary" onClick={confirm}>최종 정산 확정</button>}</div>{settlement.closedAt && <p>멤버 #{settlement.closedByMemberId} · {formatDate(settlement.closedAt)} 확정</p>}</Card><Card><h2>개인별 결제·부담 금액</h2><div className="table-wrap"><table><thead><tr><th>멤버</th><th>결제 총액</th><th>부담 총액</th><th>차액</th></tr></thead><tbody>{settlement.balances.map((balance) => <tr key={balance.memberId}><td>멤버 #{balance.memberId}</td><td>{formatMoney(balance.paidAmount)}</td><td>{formatMoney(balance.shareAmount)}</td><td>{formatMoney(balance.paidAmount - balance.shareAmount)}</td></tr>)}</tbody></table></div></Card><Card><h2>송금 관계</h2><div className="list">{settlement.transfers.map((transfer, index) => <div className="list-item" key={`${transfer.senderId}-${transfer.recipientId}-${index}`}><div><strong>멤버 #{transfer.senderId} → 멤버 #{transfer.recipientId}</strong><p>보낼 금액</p></div><strong>{formatMoney(transfer.amount)}</strong></div>)}</div>{settlement.transfers.length === 0 && <Message tone="success">추가로 송금할 금액이 없습니다.</Message>}</Card></div>}</AppShell>;
}
