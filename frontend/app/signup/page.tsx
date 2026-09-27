"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type SubmitEvent } from "react";
import { Card, Field, Message } from "@/app/_components/ui";
import { apiFetch, jsonBody } from "@/app/_lib/api";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const LABELS = { email: "이메일", nickname: "닉네임" };

export default function SignupPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  // 중복확인을 통과한 값. 입력값이 이 값과 달라지면 다시 확인해야 한다.
  const [checkedEmail, setCheckedEmail] = useState<string | null>(null);
  const [checkedNickname, setCheckedNickname] = useState<string | null>(null);

  async function check(kind: "email" | "nickname") {
    setMessage("");
    setError("");

    const value = (kind === "email" ? email : nickname).trim();

    if (!value) {
      setError(`${LABELS[kind]}을 입력해 주세요.`);
      return;
    }

    if (kind === "email" && !EMAIL_PATTERN.test(value)) {
      setError("이메일 형식이 올바르지 않습니다.");
      return;
    }

    try {
      const data = await apiFetch<{ available: boolean }>(
        `/api/user/check-${kind}?${kind}=${encodeURIComponent(value)}`
      );

      if (!data.available) {
        setError(`이미 사용 중인 ${LABELS[kind]}입니다.`);
        return;
      }

      if (kind === "email") setCheckedEmail(value);
      else setCheckedNickname(value);
      
      setMessage(`사용할 수 있는 ${LABELS[kind]}입니다.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "확인하지 못했습니다.");
    }
  }

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    setError("");
    if (checkedEmail !== email.trim()) {
      setError("이메일 중복확인을 해주세요.");
      return;
    }
    if (checkedNickname !== nickname.trim()) {
      setError("닉네임 중복확인을 해주세요.");
      return;
    }
    const values = new FormData(event.currentTarget);
    try {
      await apiFetch<{ userId: number }>("/api/user/sign-up", {
        method: "POST",
        body: jsonBody({
          email: checkedEmail,
          nickname: checkedNickname,
          password: values.get("password"),
          confirmPassword: values.get("confirmPassword"),
        }),
      });
      router.push("/login");
    } catch (e) {
      setError(e instanceof Error ? e.message : "가입하지 못했습니다.");
    }
  }

  return (
    <main className="auth-page">
      <Card className="auth-card">
        <Link href="/" className="brand">
          <span>M</span>MOIM
        </Link>
        <h1>회원가입</h1>
        <p>모임 준비를 한곳에서 시작해 보세요.</p>

        <form className="stack" onSubmit={submit}>
          <Field label="이메일">
            <div className="row">
              <input
                className="input"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <button type="button" className="button button-ghost" onClick={() => check("email")}>
                중복확인
              </button>
            </div>
          </Field>

          <Field label="닉네임">
            <div className="row">
              <input
                className="input"
                required
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
              />
              <button type="button" className="button button-ghost" onClick={() => check("nickname")}>
                중복확인
              </button>
            </div>
          </Field>

          <Field label="비밀번호" hint="영문, 숫자, 특수문자를 포함해 8자 이상">
            <input className="input" name="password" type="password" required />
          </Field>

          <Field label="비밀번호 확인">
            <input className="input" name="confirmPassword" type="password" required />
          </Field>

          {message && <Message tone="success">{message}</Message>}
          {error && <Message tone="error">{error}</Message>}

          <button className="button button-primary">회원가입 완료</button>
        </form>

        <p className="auth-footer">
          이미 계정이 있나요? <Link href="/login">로그인</Link>
        </p>
      </Card>
    </main>
  );
}
