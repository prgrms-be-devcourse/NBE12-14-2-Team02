"use client";
import { useParams } from "next/navigation";
import ExpenseForm from "@/app/_components/ExpenseForm";

export default function ExpenseCreatePage() {
  const { meetingId } = useParams<{ meetingId: string }>();
  return <ExpenseForm key={meetingId} meetingId={meetingId} />;
}