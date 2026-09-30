"use client";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState, type SubmitEvent } from "react";
import { Card, Field, Message } from "@/app/_components/ui";
import { apiFetch, jsonBody, setAccessToken } from "@/app/_lib/api";
import { REDIRECT_PARAM, safeRedirect, withRedirect } from "@/app/_lib/redirect";

export default function LoginPage() {
  return (
    <main className="auth-page">
      <Card className="auth-card">
        <Link href="/" className="brand">
          <span>M</span>MOIM
        </Link>

        {/* useSearchParams는 프리렌더를 막으므로 폼만 Suspense 안으로 내린다 */}
        <Suspense fallback={<LoginFormFallback />}>
          <LoginForm />
        </Suspense>
      </Card>
    </main>
  );
}

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // 초대 링크처럼 인증이 필요한 화면에서 넘어온 경우 로그인 후 그 화면으로 돌려보낸다
  const redirect = safeRedirect(searchParams.get(REDIRECT_PARAM));

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
      router.push(redirect ?? "/");
    } catch (e) {
      setError(e instanceof Error ? e.message : "로그인하지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
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
        {/* 가입 후에도 원래 가려던 화면으로 돌아갈 수 있게 redirect를 이어서 넘긴다 */}
        처음이신가요? <Link href={withRedirect("/signup", redirect)}>회원가입</Link>
      </p>
    </>
  );
}

// Suspense 동안에도 카드 높이가 유지되도록 폼 자리를 비워둔다
function LoginFormFallback() {
  return <div className="stack" aria-hidden />;
}
