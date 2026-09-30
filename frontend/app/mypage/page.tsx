"use client";
import Link from "next/link";
import { useEffect, useState, type SubmitEvent } from "react";
import { useRouter } from "next/navigation";
import AppShell from "@/app/_components/AppShell";
import SettlementHistory from "@/app/_components/SettlementHistory";
import { Card, EmptyState, Field, Message, PageTitle } from "@/app/_components/ui";
import { apiFetch, clearAccessToken, jsonBody } from "@/app/_lib/api";
import type { Meeting } from "@/app/_lib/types";

type Profile = { email: string; nickname: string };
type Notice = { tone: "success" | "error"; text: string } | null;

export default function MyPage() {
  const router = useRouter();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState("");
  const [completedMeetings, setCompletedMeetings] = useState<Meeting[]>([]);
  const [meetingsLoading, setMeetingsLoading] = useState(true);
  const [meetingsError, setMeetingsError] = useState("");

  // 닉네임 변경 상태: 입력 가능 여부, 입력값, 중복확인을 통과한 값, 결과 메시지
  const [nicknameEditing, setNicknameEditing] = useState(false);
  const [nickname, setNickname] = useState("");
  const [checkedNickname, setCheckedNickname] = useState<string | null>(null);
  const [nicknameNotice, setNicknameNotice] = useState<Notice>(null);

  // 비밀번호 변경 결과 메시지, 요청 중 여부
  const [passwordNotice, setPasswordNotice] = useState<Notice>(null);
  const [saving, setSaving] = useState(false);

  // 내 정보(이메일, 닉네임) 조회
  useEffect(() => {
    apiFetch<Profile>("/api/user/me")
      .then(setProfile)
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    let active = true;
    apiFetch<Meeting[]>("/api/meetings")
      .then((meetings) => {
        if (active) setCompletedMeetings(meetings.filter((meeting) => meeting.status === "COMPLETED"));
      })
      .catch((cause) => {
        if (active) setMeetingsError(cause instanceof Error ? cause.message : "완료된 모임을 불러오지 못했습니다.");
      })
      .finally(() => { if (active) setMeetingsLoading(false); });
    return () => { active = false; };
  }, []);

  // 보기/수정 모드 전환: 입력값과 이전 메시지를 초기화한다
  function changeMode(next: boolean) {
    setEditing(next);
    toggleNicknameEditing(false);
    setPasswordNotice(null);
  }

  // 닉네임 입력 가능 여부 전환: 취소하면 입력값을 현재 닉네임으로 되돌린다
  function toggleNicknameEditing(next: boolean) {
    setNicknameEditing(next);
    setNickname(profile?.nickname ?? "");
    setCheckedNickname(null);
    setNicknameNotice(null);
  }

  // 닉네임 입력: 값이 바뀌면 이전 확인 결과는 무효가 된다
  function changeNickname(value: string) {
    setNickname(value);
    setNicknameNotice(null);
  }

  const trimmedNickname = nickname.trim();
  const nicknameChanged = trimmedNickname !== "" && trimmedNickname !== profile?.nickname;
  const nicknameChecked = nicknameChanged && checkedNickname === trimmedNickname;

  // 닉네임 중복확인
  async function checkNickname() {
    setSaving(true);
    try {
      const data = await apiFetch<{ available: boolean }>(
        `/api/user/check-nickname?nickname=${encodeURIComponent(trimmedNickname)}`
      );
      if (!data.available) {
        setNicknameNotice({ tone: "error", text: "이미 사용 중인 닉네임입니다." });
        return;
      }
      setCheckedNickname(trimmedNickname);
      setNicknameNotice({ tone: "success", text: "사용할 수 있는 닉네임입니다." });
    } catch (e) {
      setNicknameNotice({ tone: "error", text: e instanceof Error ? e.message : "확인하지 못했습니다." });
    } finally {
      setSaving(false);
    }
  }

  // 닉네임 변경 (중복확인을 통과한 값만 보낸다)
  async function updateNickname() {
    setSaving(true);
    try {
      const updated = await apiFetch<Profile>("/api/user/me/nickname", {
        method: "PATCH",
        body: jsonBody({ nickname: checkedNickname }),
      });
      setProfile(updated);
      setNickname(updated.nickname);
      setCheckedNickname(null);
      setNicknameEditing(false);
      setNicknameNotice({ tone: "success", text: "닉네임을 변경했습니다." });
    } catch (e) {
      // 확인 이후 다른 사람이 같은 닉네임을 먼저 사용했을 수 있으므로 다시 확인하게 한다
      setCheckedNickname(null);
      setNicknameNotice({ tone: "error", text: e instanceof Error ? e.message : "변경하지 못했습니다." });
    } finally {
      setSaving(false);
    }
  }

  // 비밀번호 변경 (현재 비밀번호는 서버에서 확인한다)
  async function updatePassword(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    // await 이후에는 currentTarget이 null이 되므로 미리 담아둔다
    const formEl = event.currentTarget;
    const form = new FormData(formEl);

    setSaving(true);
    setPasswordNotice(null);
    try {
      await apiFetch<void>("/api/user/me/password", {
        method: "PATCH",
        body: jsonBody({ password: form.get("password"), newPassword: form.get("newPassword") }),
      });
      formEl.reset();
      setPasswordNotice({ tone: "success", text: "비밀번호를 변경했습니다." });
    } catch (e) {
      setPasswordNotice({ tone: "error", text: e instanceof Error ? e.message : "변경하지 못했습니다." });
    } finally {
      setSaving(false);
    }
  }

  // 회원 탈퇴: 확인을 받은 뒤 요청하고, 성공하면 토큰을 지우고 로그인 페이지로 이동
  async function withdraw() {
    if (!confirm("정말 탈퇴하시겠습니까? 탈퇴 후에는 되돌릴 수 없습니다.")) return;

    setSaving(true);
    try {
      await apiFetch<void>("/api/user", { method: "DELETE" });
      clearAccessToken();
      router.push("/login");
    } catch (e) {
      setError(e instanceof Error ? e.message : "탈퇴하지 못했습니다.");
      setSaving(false);
    }
  }

  return (
    <AppShell>
      <PageTitle
        eyebrow="My page"
        title="마이페이지"
        description="계정 정보를 확인하고 변경하세요."
        action={
          editing && (
            <button className="button button-ghost" onClick={() => changeMode(false)} disabled={saving}>
              수정 완료
            </button>
          )
        }
      />
      {error && <Message tone="error">{error}</Message>}

      {!profile ? (
        !error && <Message>계정 정보를 불러오는 중입니다.</Message>
      ) : editing ? (
        <div className="grid grid-2">
          <Card>
            <h2>닉네임 변경</h2>
            <div className="stack">
              <Field label="이메일" hint="이메일은 변경할 수 없습니다.">
                <input className="input" disabled value={profile.email} />
              </Field>
              <Field label="닉네임">
                <div className="row">
                  <input
                    // 입력 가능해질 때 다시 그려서 autoFocus로 바로 입력할 수 있게 한다
                    key={nicknameEditing ? "editing" : "locked"}
                    className="input"
                    value={nickname}
                    onChange={(e) => changeNickname(e.target.value)}
                    disabled={!nicknameEditing}
                    autoFocus={nicknameEditing}
                  />
                  {/* 버튼 영역 너비를 고정해 버튼이 바뀌어도 입력칸 크기가 유지된다 */}
                  <div className="nickname-actions">
                    {!nicknameEditing ? (
                      <button type="button" className="button button-secondary" onClick={() => toggleNicknameEditing(true)}>
                        닉네임 변경
                      </button>
                    ) : (
                      <>
                        {/* 새 닉네임을 입력해야 중복확인이 활성화되고, 확인을 통과하면 변경 버튼으로 바뀐다 */}
                        {nicknameChecked ? (
                          <button type="button" className="button button-primary" onClick={updateNickname} disabled={saving}>
                            변경
                          </button>
                        ) : (
                          <button type="button" className="button button-ghost" onClick={checkNickname} disabled={saving || !nicknameChanged}>
                            중복확인
                          </button>
                        )}
                        <button type="button" className="button button-ghost" onClick={() => toggleNicknameEditing(false)} disabled={saving}>
                          취소
                        </button>
                      </>
                    )}
                  </div>
                </div>
                {nicknameNotice && (
                  <small className={nicknameNotice.tone === "error" ? "field-error" : "field-success"}>
                    {nicknameNotice.text}
                  </small>
                )}
              </Field>
            </div>
          </Card>

          <Card>
            <h2>비밀번호 변경</h2>
            <form className="stack" onSubmit={updatePassword}>
              <Field label="현재 비밀번호">
                <input className="input" name="password" type="password" autoComplete="current-password" required />
              </Field>
              <Field label="새 비밀번호" hint="영문, 숫자, 특수문자를 포함해 8자 이상">
                <input className="input" name="newPassword" type="password" autoComplete="new-password" required />
              </Field>
              {passwordNotice && <Message tone={passwordNotice.tone}>{passwordNotice.text}</Message>}
              <button className="button button-primary" disabled={saving}>비밀번호 변경</button>
            </form>
          </Card>
        </div>
      ) : (
        <Card>
          <div className="row between">
            <h2>기본 계정 정보</h2>
            <button className="button button-secondary" onClick={() => changeMode(true)}>
              수정하기
            </button>
          </div>
          <dl className="profile-list">
            <dt>이메일</dt>
            <dd>{profile.email}</dd>
            <dt>닉네임</dt>
            <dd>{profile.nickname}</dd>
          </dl>
        </Card>
      )}

      <div className="top-gap"><SettlementHistory /></div>
      <div className="top-gap">
        <Card>
          <h2>완료된 모임</h2>
          <p>종료된 모임의 내용을 다시 확인할 수 있습니다.</p>
          {meetingsError && <Message tone="error">{meetingsError}</Message>}
          {meetingsLoading ? (
            <Message>완료된 모임을 불러오는 중입니다.</Message>
          ) : completedMeetings.length === 0 && !meetingsError ? (
            <EmptyState title="완료된 모임이 없어요" description="모임이 종료되면 이곳에서 확인할 수 있습니다." />
          ) : (
            <div className="list">
              {completedMeetings.map((meeting) => (
                <div className="list-item" key={meeting.id}>
                  <div>
                    <h3>{meeting.name}</h3>
                    <p>{meeting.description || "등록된 설명이 없습니다."} · 참여자 {meeting.participantCount}명</p>
                  </div>
                  <Link className="button button-secondary button-small" href={`/meetings/${meeting.id}`}>상세보기</Link>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      <div className="top-gap">
        <Card>
          <div className="row between">
            <div>
              <h3>회원 탈퇴</h3>
              <p>계정을 삭제하고 서비스 이용을 종료합니다.</p>
            </div>
            <button className="button button-danger" onClick={withdraw} disabled={saving}>회원 탈퇴</button>
          </div>
        </Card>
      </div>
    </AppShell>
  );
}
