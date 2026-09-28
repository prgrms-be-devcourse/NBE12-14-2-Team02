"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type SubmitEvent } from "react";
import { Card, Field, Message } from "@/app/_components/ui";
import { apiFetch, jsonBody, setAccessToken } from "@/app/_lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // 로그인 제출
  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    // 요청 중에는 버튼을 비활성화해서 중복 제출을 막는다
    setLoading(true);
    setError("");
    const values = new FormData(event.currentTarget);

    try {
      const response = await apiFetch<{ accessToken: string }>("/api/auth/log-in", {
        method: "POST",
        body: jsonBody({
          email: values.get("email"),
          password: values.get("password"),
        }),
      });
      
      // access token은 sessionStorage에 저장, refresh token은 서버가 httpOnly 쿠키로 내려준다
      setAccessToken(response.accessToken);
      router.push("/");
    } catch (e) {
      setError(e instanceof Error ? e.message : "로그인하지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-page">
      <Card className="auth-card">
        <Link href="/" className="brand">
          <span>M</span>MOIM
        </Link>

        <form className="stack" onSubmit={submit}>
          <Field label="이메일">
            <input className="input" name="email" type="email" required placeholder="name@example.com" />
          </Field>

          <Field label="비밀번호">
            <input className="input" name="password" type="password" required placeholder="비밀번호" />
          </Field>

          {error && <Message tone="error">{error}</Message>}

          <button className="button button-primary" disabled={loading}>
            {loading ? "로그인 중..." : "로그인"}
          </button>
        </form>

        <p className="auth-footer">
          처음이신가요? <Link href="/signup">회원가입</Link>
        </p>
      </Card>
    </main>
  );
}
