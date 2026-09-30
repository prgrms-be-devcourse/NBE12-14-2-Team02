"use client";

import { useEffect, useId, useRef, useState } from "react";
import { formatMoney } from "./ui";
import styles from "./ExpensePresentation.module.css";

export default function SettlementSummaryLine({ direction, transfers, memberName }: {
  direction: "receive" | "send";
  transfers: Array<{ senderId: number; recipientId: number; amount: number }>;
  memberName: (id: number) => string;
}) {
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const [pinned, setPinned] = useState(false);
  const [dismissed, setDismissed] = useState(false);
  const container = useRef<HTMLSpanElement>(null);
  const detailsId = useId();
  const open = !dismissed && (hovered || focused || pinned);
  const receiving = direction === "receive";
  // 건수가 아닌 실제 상대 인원수로 요약합니다.
  const amounts = new Map<number, number>();
  for (const transfer of transfers) {
    if (transfer.amount <= 0) continue;
    const id = receiving ? transfer.senderId : transfer.recipientId;
    amounts.set(id, (amounts.get(id) ?? 0) + transfer.amount);
  }
  const people = [...amounts].map(([id, amount]) => ({ id, amount }));
  const total = people.reduce((sum, person) => sum + person.amount, 0);

  useEffect(() => {
    if (!open) return;
    function closeOutside(event: PointerEvent) {
      if (event.target instanceof Node && !container.current?.contains(event.target)) {
        setPinned(false); setHovered(false); setFocused(false); setDismissed(true);
      }
    }
    document.addEventListener("pointerdown", closeOutside);
    return () => document.removeEventListener("pointerdown", closeOutside);
  }, [open]);

  if (!people.length) return <p className={styles.summaryLine}>{receiving ? "받을" : "보낼"} 금액이 없어요.</p>;
  const label = `${memberName(people[0].id)}${people.length > 1 ? ` 외 ${people.length - 1}명` : ""}`;
  return <p className={styles.summaryLine}>
    <span ref={container} className={styles.summaryPeople}
      onPointerEnter={event => { if (event.pointerType === "mouse") { setHovered(true); setDismissed(false); } }}
      onPointerLeave={() => setHovered(false)}
      onKeyDown={event => { if (event.key === "Escape") { setPinned(false); setDismissed(true); } }}>
      <button type="button" className={styles.peopleButton} aria-expanded={open} aria-controls={detailsId}
        onFocus={event => { if (event.currentTarget.matches(":focus-visible")) { setFocused(true); setDismissed(false); } }}
        onBlur={() => { setFocused(false); setPinned(false); }}
        onClick={() => { setPinned(!open); setDismissed(open); }}>
        <strong>{label}</strong>
      </button>
      <span id={detailsId} className={styles.peoplePopover} hidden={!open}>
        <strong>{receiving ? "나에게 보낼 모임원" : "내가 보낼 모임원"}</strong>
        {people.map(person => <span className={styles.personRow} key={person.id}>
          <span>{memberName(person.id)}</span><span>{formatMoney(person.amount)}</span>
        </span>)}
      </span>
    </span>에게 {people.length > 1 ? "총 " : ""}{formatMoney(total)}을 {receiving ? "받아야" : "보내야"} 해요.
  </p>;
}
