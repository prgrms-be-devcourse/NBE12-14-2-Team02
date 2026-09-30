"use client";

import { useEffect, useId, type ReactNode } from "react";

type ModalProps = {
  title: string;
  children?: ReactNode;
  confirmText?: string;
  // 취소 문구를 넘기면 취소 버튼이 함께 나온다 (알림용 모달은 확인 버튼만)
  cancelText?: string;
  tone?: "info" | "danger";
  // 확인을 넘기지 않으면 확인 버튼도 닫기만 한다
  onConfirm?: () => void;
  onClose: () => void;
};

export default function Modal({ title, children, confirmText = "확인", cancelText, tone = "info", onConfirm, onClose }: ModalProps) {
  const titleId = useId();
  // 되돌릴 수 없는 작업이면 취소 버튼에 먼저 포커스를 둬서 Enter로 실행되지 않게 한다
  const focusCancel = tone === "danger" && Boolean(cancelText);

  // Esc로 닫고, 열려 있는 동안에는 뒤 화면이 스크롤되지 않게 한다
  useEffect(() => {
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", closeOnEscape);
    const { overflow } = document.body.style;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", closeOnEscape);
      document.body.style.overflow = overflow;
    };
  }, [onClose]);

  return (
    // 오버레이(바깥 영역)를 눌렀을 때만 닫는다
    <div className="modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className={`modal modal-${tone}`} role={tone === "danger" ? "alertdialog" : "dialog"} aria-modal="true" aria-labelledby={titleId}>
        <div className="modal-heading">
          <strong id={titleId}>{title}</strong>
          {children}
        </div>
        <div className="modal-actions">
          {cancelText && (
            <button type="button" className="button button-ghost" onClick={onClose} autoFocus={focusCancel}>
              {cancelText}
            </button>
          )}
          <button
            type="button"
            className={tone === "danger" ? "button button-danger" : "button button-primary"}
            onClick={onConfirm ?? onClose}
            autoFocus={!focusCancel}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}
