"use client";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState, type SubmitEvent } from "react";
import { Card, Field, Message } from "@/app/_components/ui";
import { apiFetch, jsonBody } from "@/app/_lib/api";
import { REDIRECT_PARAM, safeRedirect, withRedirect } from "@/app/_lib/redirect";

// 아이디@도메인.최상위도메인 형태만 허용 (백엔드 @Email보다 조금 엄격함)
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
// 안내 메시지에 들어갈 필드 이름
const LABELS = { email: "이메일", nickname: "닉네임" };

export default function SignupPage() {
  return (
    <main className="auth-page">
      <Card className="auth-card">
        <Link href="/" className="brand">
          <span>M</span>MOIM
        </Link>
        <h1>회원가입</h1>
        <p>모임 준비를 한곳에서 시작해 보세요.</p>

        {/* useSearchParams는 프리렌더를 막으므로 폼만 Suspense 안으로 내린다 */}
        <Suspense fallback={<SignupFormFallback />}>
          <SignupForm />
        </Suspense>
      </Card>
    </main>
  );
}

function SignupForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  // 중복확인을 통과한 값. 입력값이 이 값과 달라지면 다시 확인해야 한다.
  const [checkedEmail, setCheckedEmail] = useState<string | null>(null);
  const [checkedNickname, setCheckedNickname] = useState<string | null>(null);

  // 초대 링크에서 넘어온 경우, 가입 후 로그인까지 끝나면 그 화면으로 돌아가야 한다
  const redirect = safeRedirect(searchParams.get(REDIRECT_PARAM));

  // 이메일/닉네임 중복확인
  async function check(kind: "email" | "nickname") {
    // 이전 확인 결과 메시지 초기화
    setMessage("");
    setError("");

    const value = (kind === "email" ? email : nickname).trim();

    // 빈 값이나 잘못된 형식은 API를 호출하지 않고 바로 막는다
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

      // 사용 가능한 경우에만 확인 통과 값으로 저장
      if (kind === "email") setCheckedEmail(value);
      else setCheckedNickname(value);

      setMessage(`사용할 수 있는 ${LABELS[kind]}입니다.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "확인하지 못했습니다.");
    }
  }

  // 회원가입 제출
  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    setError("");

    // 중복확인을 안 했거나, 확인 후 값을 바꿨으면 제출하지 않는다
    if (checkedEmail !== email.trim()) {
      setError("이메일 중복확인을 해주세요.");
      return;
    }
    if (checkedNickname !== nickname.trim()) {
      setError("닉네임 중복확인을 해주세요.");
      return;
    }

    // 비밀번호는 state 없이 폼에서 바로 읽는다
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
      // 가입만으로는 로그인되지 않으므로 로그인 페이지로 이동 (redirect는 그대로 이어서 넘긴다)
      router.push(withRedirect("/login", redirect));
    } catch (e) {
      setError(e instanceof Error ? e.message : "가입하지 못했습니다.");
    }
  }

  return (
    <>
      <form className="stack" onSubmit={submit}>
        {/* 중복확인 버튼은 type="button"이라 폼을 제출하지 않는다 */}
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
        이미 계정이 있나요? <Link href={withRedirect("/login", redirect)}>로그인</Link>
      </p>
    </>
  );
}

// Suspense 동안에도 카드 높이가 유지되도록 폼 자리를 비워둔다
function SignupFormFallback() {
  return <div className="stack" aria-hidden />;
}
