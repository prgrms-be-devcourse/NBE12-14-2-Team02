"use client";

type ConfirmModalProps = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  loading?: boolean;
  danger?: boolean;
  onConfirm: () => void;
  onClose: () => void;
};

export default function ConfirmModal({
 open,
 title,
 description,
 confirmLabel,
 loading = false,
 danger = false,
 onConfirm,
 onClose,
}: ConfirmModalProps) {
  if (!open) {
    return null;
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
        <div
            className={`modal ${danger ? "modal-danger" : ""}`}
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirm-modal-title"
        >
          <div className="modal-heading">
            <strong id="confirm-modal-title">
              {title}
            </strong>

            <p>{description}</p>
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
                type="button"
                className={
                  danger
                      ? "button button-danger"
                      : "button button-primary"
                }
                onClick={onConfirm}
                disabled={loading}
            >
              {loading ? "처리 중..." : confirmLabel}
            </button>
          </div>
        </div>
      </div>
  );
}