import { apiFetch, ApiError } from "./api";
import type { Settlement } from "./types";

export type Expense = {
  id: number; meetingId: number; payerMemberId: number; title: string;
  amount: number; memo: string | null; splitMode: "EQUAL" | "EXACT" | null;
  roundingUnit: number | null; remainderMemberId: number | null;
  participants: Array<{ memberId: number; amount: number }>; createdAt: string;
};
export type ExpenseList = {
  currentMemberId: number; leader: boolean; editable: boolean; expenses: Expense[];
};

// 404만 '아직 확정되지 않음'으로 처리하고 인증·서버 오류는 화면에 전달합니다.
export async function getSettlement(meetingId: string): Promise<Settlement | null> {
  try {
    return await apiFetch<Settlement>(`/api/meetings/${meetingId}/settlement`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}