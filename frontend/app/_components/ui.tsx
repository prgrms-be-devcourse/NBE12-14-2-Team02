import type { ReactNode } from "react";

export function PageTitle({ eyebrow, title, description, action }: { eyebrow?: string; title: string; description?: string; action?: ReactNode }) {
  return <div className="page-title"><div>{eyebrow && <p className="eyebrow">{eyebrow}</p>}<h1>{title}</h1>{description && <p>{description}</p>}</div>{action}</div>;
}
export function Card({ children, className = "" }: { children: ReactNode; className?: string }) { return <section className={`card ${className}`}>{children}</section>; }
export function Badge({ children, tone = "purple" }: { children: ReactNode; tone?: "purple" | "green" | "gray" | "coral" }) { return <span className={`badge badge-${tone}`}>{children}</span>; }
export function EmptyState({ title, description }: { title: string; description: string }) { return <div className="empty-state"><strong>{title}</strong><p>{description}</p></div>; }
export function Message({ children, tone = "info" }: { children: ReactNode; tone?: "info" | "error" | "success" }) { return <div className={`message message-${tone}`}>{children}</div>; }
export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) { return <label className="field"><span>{label}</span>{children}{hint && <small>{hint}</small>}</label>; }
export function formatDate(value?: string | null) { if (!value) return "-"; return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: value.includes("T") ? "short" : undefined }).format(new Date(value)); }
export function formatMoney(value: number) { return `${new Intl.NumberFormat("ko-KR").format(value)}원`; }
