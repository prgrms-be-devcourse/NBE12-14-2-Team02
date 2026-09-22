"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Card, Field, Message } from "@/app/_components/ui";
import { apiFetch, jsonBody } from "@/app/_lib/api";

export default function SignupPage() {
  const router = useRouter(); const [email, setEmail] = useState(""); const [nickname, setNickname] = useState(""); const [message, setMessage] = useState(""); const [error, setError] = useState("");
  async function check(kind: "email" | "nickname") { try { const value = kind === "email" ? email : nickname; const data = await apiFetch<{ available: boolean }>(`/api/user/check-${kind}?${kind}=${encodeURIComponent(value)}`); setMessage(data.available ? "사용할 수 있습니다." : "이미 사용 중입니다."); } catch (e) { setError(e instanceof Error ? e.message : "확인하지 못했습니다."); } }
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setError(""); const values = new FormData(event.currentTarget); try { await apiFetch<{ userId: number }>("/api/user/sign-up", { method: "POST", body: jsonBody({ email, nickname, password: values.get("password"), confirmPassword: values.get("confirmPassword") }) }); router.push("/login"); } catch (e) { setError(e instanceof Error ? e.message : "가입하지 못했습니다."); } }
  return <main className="auth-page"><Card className="auth-card"><Link href="/" className="brand"><span>M</span>MOIM</Link><h1>회원가입</h1><p>모임 준비를 한곳에서 시작해 보세요.</p><form className="stack" onSubmit={submit}><Field label="이메일"><div className="row"><input className="input" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /><button type="button" className="button button-ghost" onClick={() => check("email")}>중복확인</button></div></Field><Field label="닉네임"><div className="row"><input className="input" required value={nickname} onChange={(e) => setNickname(e.target.value)} /><button type="button" className="button button-ghost" onClick={() => check("nickname")}>중복확인</button></div></Field><Field label="비밀번호" hint="영문, 숫자, 특수문자를 포함해 8자 이상"><input className="input" name="password" type="password" required /></Field><Field label="비밀번호 확인"><input className="input" name="confirmPassword" type="password" required /></Field>{message && <Message tone="success">{message}</Message>}{error && <Message tone="error">{error}</Message>}<button className="button button-primary">회원가입 완료</button></form><p className="auth-footer">이미 계정이 있나요? <Link href="/login">로그인</Link></p></Card></main>;
}
