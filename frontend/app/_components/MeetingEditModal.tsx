"use client";

import {
  type FormEvent,
  useState,
} from "react";

import { Field } from "@/app/_components/ui";

type MeetingEditModalProps = {
  open: boolean;
  name: string;
  description?: string | null;
  loading?: boolean;
  onClose: () => void;
  onSubmit: (values: {
    name: string;
    description: string;
  }) => void;
};

export default function MeetingEditModal({
 open,
 name,
 description,
 loading = false,
 onClose,
 onSubmit,
}: MeetingEditModalProps) {
  if (!open) {
    return null;
  }

  return (
      <MeetingEditModalContent
          name={name}
          description={description}
          loading={loading}
          onClose={onClose}
          onSubmit={onSubmit}
      />
  );
}

function MeetingEditModalContent({
 name: initialName,
 description: initialDescription,
 loading,
 onClose,
 onSubmit,
}: Omit<MeetingEditModalProps, "open">) {
  const [name, setName] = useState(initialName);
  const [description, setDescription] =
      useState(initialDescription ?? "");

  function handleSubmit(
      event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    const trimmedName = name.trim();

    if (!trimmedName) {
      return;
    }

    onSubmit({
      name: trimmedName,
      description: description.trim(),
    });
  }

  return (
      <div
          className="modal-overlay"
          role="presentation"
          onMouseDown={(event) => {
            if (
                event.target === event.currentTarget &&
                !loading
            ) {
              onClose();
            }
          }}
      >
        <form
            className="modal"
            role="dialog"
            aria-modal="true"
            onSubmit={handleSubmit}
        >
          <div className="modal-heading">
            <strong>모임 정보 수정</strong>
            <p>
              모임 이름과 설명을 수정할 수 있습니다.
            </p>
          </div>

          <div className="stack top-gap">
            <Field
                label="모임 이름"
                hint={`${name.length}/100`}
            >
              <input
                  className="input"
                  value={name}
                  maxLength={100}
                  required
                  onChange={(event) =>
                      setName(event.target.value)
                  }
              />
            </Field>

            <Field
                label="모임 설명"
                hint={`${description.length}/500`}
            >
            <textarea
                className="textarea"
                value={description}
                maxLength={500}
                rows={5}
                onChange={(event) =>
                    setDescription(event.target.value)
                }
            />
            </Field>
          </div>

          <div className="modal-actions">
            <button
                type="button"
                className="button button-ghost"
                onClick={onClose}
                disabled={loading}
            >
              취소
            </button>

            <button
                type="submit"
                className="button button-primary"
                disabled={loading || !name.trim()}
            >
              {loading ? "수정 중..." : "수정하기"}
            </button>
          </div>
        </form>
      </div>
  );
}