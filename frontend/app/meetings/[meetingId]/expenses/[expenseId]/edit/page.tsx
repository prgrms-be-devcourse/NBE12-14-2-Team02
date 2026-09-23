"use client";
import { useParams } from "next/navigation";
import ExpenseForm from "@/app/_components/ExpenseForm";

export default function ExpenseEditPage() {
  const { meetingId, expenseId } = useParams<{ meetingId: string; expenseId: string }>();
  return <ExpenseForm key={`${meetingId}-${expenseId}`} meetingId={meetingId} expenseId={expenseId} />;
}