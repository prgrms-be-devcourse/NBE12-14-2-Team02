"use client";

import type { InputHTMLAttributes } from "react";
import styles from "./ExpensePresentation.module.css";

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, "value" | "onChange" | "type"> & {
  value: string;
  onValueChange: (value: string) => void;
};

export default function ExpenseMoneyInput({ value, onValueChange, className, ...props }: Props) {
  const formatted = value.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return <span className={`${styles.moneyInput} ${className ?? ""}`}>
    <input {...props} className="input" type="text" inputMode="numeric" autoComplete="off"
      value={formatted} placeholder="0" onChange={event => {
        const input = event.currentTarget;
        const text = input.value;
        // 쉼표만 표시용으로 제거합니다. 음수·소수 입력을 다른 금액으로 바꾸지 않습니다.
        if (!/^[\d,]*$/.test(text)) return;
        const digits = text.replace(/,/g, "").replace(/^0+(?=\d)/, "");
        if (digits.length > 12) return;
        const beforeCursor = text.slice(0, input.selectionStart ?? text.length).replace(/,/g, "");
        const digitCount = beforeCursor.replace(/^0+(?=\d)/, "").length;
        onValueChange(digits);
        requestAnimationFrame(() => {
          if (document.activeElement !== input) return;
          let position = 0;
          let count = 0;
          while (position < input.value.length && count < digitCount) {
            if (/\d/.test(input.value[position])) count++;
            position++;
          }
          input.setSelectionRange(position, position);
        });
      }} onKeyDown={event => {
        const input = event.currentTarget;
        const cursor = input.selectionStart ?? 0;
        if (cursor !== input.selectionEnd) return;
        // 구분자 앞뒤에서 삭제 키를 누를 때 커서가 쉼표에 갇히지 않게 합니다.
        if (event.key === "Backspace" && input.value[cursor - 1] === ",") {
          input.setSelectionRange(cursor - 1, cursor - 1);
        } else if (event.key === "Delete" && input.value[cursor] === ",") {
          input.setSelectionRange(cursor + 1, cursor + 1);
        }
      }} />
    <span className={styles.currency} aria-hidden="true">원</span>
  </span>;
}
