export type Meeting = { id: number; hostId: number; name: string; description: string | null; status: string; participantCount: number };
export type MeetingMember = { id: number; meetingId: number; userId: number; nickname: string; status: string; joinedAt: string; leftAt: string | null };
export type ScheduleCandidate = { id: number; candidateDate: string };
export type SchedulePoll = { id: number; meetingId: number; deadline: string; status: "OPEN" | "CLOSED"; candidates: ScheduleCandidate[] };
export type ScheduleResult = {
  candidateRanks: Array<{ candidateId: number; candidateDate: string; totalScore: number; rank: number; responseCount: number; nonResponseCount: number; preferenceCounts: { prefer: number; available: number; dislike: number; impossible: number } }>;
  participantResponses: Array<{ meetingMemberId: number; userId: number; nickname: string; answers: Array<{ candidateId: number; candidateDate: string; preference: "PREFER" | "AVAILABLE" | "DISLIKE" | "IMPOSSIBLE" | null }> }>;
};
export type ContentPoll = {
  id: number; meetingId: number; deadline: string; status: "OPEN" | "CLOSED";
  candidates: Array<{ candidateId: number; createdByMemberId: number; title: string; description: string | null; totalScore: number; myPreference: "PREFER" | "AVAILABLE" | "DISLIKE" | null }>;
};
export type ContentResults = {
  id: number; meetingId: number; deadline: string; status: "OPEN" | "CLOSED"; confirmedCandidateId: number | null; joinedCount: number;
  candidates: Array<{ candidateId: number; createdByMemberId: number; createdByNickname: string; title: string; description: string | null; totalScore: number; preferCount: number; availableCount: number; dislikeCount: number; responseCount: number; noResponseCount: number }>;
  members: Array<{ meetingMemberId: number; userId: number; nickname: string; host: boolean; preferences: Array<"PREFER" | "AVAILABLE" | "DISLIKE" | null>; responseCount: number }>;
};
export type NotificationItem = { id: number; meetingId: number | null; type: string; title: string; content: string; redirectUrl: string | null; isRead: boolean; createdAt: string };
export type Settlement = { settlementId: number; meetingId: number; status: "OPEN" | "CLOSED"; closedByMemberId: number | null; closedAt: string | null; balances: Array<{ memberId: number; paidAmount: number; shareAmount: number }>; transfers: Array<{ senderId: number; recipientId: number; amount: number }> };
