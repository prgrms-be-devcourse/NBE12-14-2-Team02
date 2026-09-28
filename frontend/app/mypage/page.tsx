"use client";
import { useEffect, useState, type SubmitEvent } from "react";
import { useRouter } from "next/navigation";
import AppShell from "@/app/_components/AppShell";
import { Card, Field, Message, PageTitle } from "@/app/_components/ui";
import { apiFetch, clearAccessToken, jsonBody } from "@/app/_lib/api";

type Profile = { email: string; nickname: string };

export default function MyPage() {
  const router = useRouter();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  // 내 정보(이메일, 닉네임) 조회
  useEffect(() => {
    apiFetch<Profile>("/api/user/me")
      .then(setProfile)
      .catch((e) => setError(e.message));
  }, []);

  // 닉네임 변경
  async function nickname(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
      const updated = await apiFetch<Profile>("/api/user/me/nickname", {
        method: "PATCH",
        body: jsonBody({ newNickname: form.get("nickname") }),
      });
      setProfile(updated);
      setMessage("닉네임을 변경했습니다.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "변경하지 못했습니다.");
    }
  }

  // 비밀번호 변경 (현재 비밀번호 확인 후 새 비밀번호로 변경)
  async function password(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
      await apiFetch<void>("/api/user/me/password", {
        method: "PATCH",
        body: jsonBody({
          password: form.get("password"),
          newPassword: form.get("newPassword"),
        }),
      });
      setMessage("비밀번호를 변경했습니다.");
      event.currentTarget.reset();
    } catch (e) {
      setError(e instanceof Error ? e.message : "변경하지 못했습니다.");
    }
  }

  // 로그아웃: 서버 요청 성공 여부와 관계없이 토큰을 지우고 로그인 페이지로 이동
  async function logout() {
    try {
      await apiFetch<void>("/api/auth/logout", { method: "POST" });
    } catch {
      /* 이미 인증이 없으면 로그아웃된 상태로 본다 */
    } finally {
      clearAccessToken();
      router.push("/login");
    }
  }

  return (
    <AppShell>
      <PageTitle eyebrow="My page" title="마이페이지" description="계정 정보를 확인하고 변경하세요." />
      {message && <Message tone="success">{message}</Message>}
      {error && <Message tone="error">{error}</Message>}

      <div className="grid grid-2">
        <Card>
          <h2>기본 계정 정보</h2>
          <form className="stack" onSubmit={nickname}>
            <Field label="이메일">
              <input className="input" disabled value={profile?.email || ""} />
            </Field>
            <Field label="닉네임">
              <input
                className="input"
                name="nickname"
                defaultValue={profile?.nickname || ""}
                // 프로필을 불러오거나 닉네임이 바뀌면 key가 달라져 입력칸이 새 값으로 다시 그려진다
                key={profile?.nickname}
                required
              />
            </Field>
            <button className="button button-primary">닉네임 수정</button>
          </form>
        </Card>

        <Card>
          <h2>비밀번호 변경</h2>
          <form className="stack" onSubmit={password}>
            <Field label="현재 비밀번호">
              <input className="input" name="password" type="password" required />
            </Field>
            <Field label="새 비밀번호" hint="영문, 숫자, 특수문자를 포함해 8자 이상">
              <input className="input" name="newPassword" type="password" required />
            </Field>
            <button className="button button-primary">비밀번호 변경</button>
          </form>
        </Card>
      </div>

      <div className="top-gap">
        <Card>
          <div className="row between">
            <div>
              <h3>로그아웃</h3>
              <p>현재 브라우저의 로그인 상태를 종료합니다.</p>
            </div>
            <button className="button button-ghost" onClick={logout}>로그아웃</button>
          </div>
        </Card>
      </div>
    </AppShell>
  );
}
