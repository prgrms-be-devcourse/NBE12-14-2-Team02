import Link from "next/link";

export default function MeetingTabs({ meetingId, active }: { meetingId: string | number; active: "overview" | "schedule" | "content" | "settlement" }) {
  const tabs = [{ key: "overview", icon: "⌂", label: "개요", href: `/meetings/${meetingId}` }, { key: "schedule", icon: "◷", label: "일정 투표", href: `/meetings/${meetingId}/schedule` }, { key: "content", icon: "◇", label: "콘텐츠 투표", href: `/meetings/${meetingId}/content` }, { key: "settlement", icon: "₩", label: "정산", href: `/meetings/${meetingId}/settlement` }];
  return <nav className="tabs" aria-label="모임 메뉴">{tabs.map((item) => <Link key={item.key} className={active === item.key ? "active" : ""} aria-current={active === item.key ? "page" : undefined} href={item.href}><span aria-hidden="true">{item.icon}</span>{item.label}</Link>)}</nav>;
}
