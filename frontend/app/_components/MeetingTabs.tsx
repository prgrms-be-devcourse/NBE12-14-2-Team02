import Link from "next/link";

export default function MeetingTabs({ meetingId, active }: { meetingId: string | number; active: "overview" | "schedule" | "content" | "settlement" }) {
  const tabs = [{ key: "overview", label: "개요", href: `/meetings/${meetingId}` }, { key: "schedule", label: "일정 투표", href: `/meetings/${meetingId}/schedule/vote` }, { key: "content", label: "콘텐츠 투표", href: `/meetings/${meetingId}/content` }, { key: "settlement", label: "정산", href: `/meetings/${meetingId}/settlement` }];
  return <div className="tabs">{tabs.map((item) => <Link key={item.key} className={active === item.key ? "active" : ""} href={item.href}>{item.label}</Link>)}</div>;
}
