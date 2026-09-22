"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Card, Field, Message } from "@/app/_components/ui";
import { apiFetch, jsonBody, setAccessToken } from "@/app/_lib/api";

export default function LoginPage() {
  const router = useRouter(); const [error, setError] = useState(""); const [loading, setLoading] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setLoading(true); setError(""); const values = new FormData(event.currentTarget); try { const response = await apiFetch<{ accessToken: string }>("/api/auth/log-in", { method: "POST", body: jsonBody({ email: values.get("email"), password: values.get("password") }) }); setAccessToken(response.accessToken); router.push("/"); } catch (e) { setError(e instanceof Error ? e.message : "로그인하지 못했습니다."); } finally { setLoading(false); } }
  return <main className="auth-page"><Card className="auth-card"><Link href="/" className="brand"><span>M</span>MOIM</Link><h1>다시 만나서 반가워요</h1><p>이메일과 비밀번호로 로그인하세요.</p><form className="stack" onSubmit={submit}><Field label="이메일"><input className="input" name="email" type="email" required placeholder="name@example.com" /></Field><Field label="비밀번호"><input className="input" name="password" type="password" required placeholder="비밀번호" /></Field>{error && <Message tone="error">{error}</Message>}<button className="button button-primary" disabled={loading}>{loading ? "로그인 중..." : "로그인"}</button></form><p className="auth-footer">처음이신가요? <Link href="/signup">회원가입</Link></p></Card></main>;
}
