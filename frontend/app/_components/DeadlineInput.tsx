"use client";

import { useState } from "react";
import { Field } from "./ui";

type PickerValue = {
  year: number;
  month: number;
  day: number;
  hour: number;
};

function pad(value: number) {
  return String(value).padStart(2, "0");
}

function parseValue(value?: string): PickerValue | null {
  if (!value) return null;
  const match = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/.exec(value);
  if (!match) return null;
  return {
    year: Number(match[1]),
    month: Number(match[2]),
    day: Number(match[3]),
    hour: Number(match[4]),
  };
}

function initialDraft(value: PickerValue | null): PickerValue {
  if (value) return value;
  const date = new Date(Date.now() + 60 * 60 * 1000);
  date.setMinutes(0, 0, 0);
  return {
    year: date.getFullYear(),
    month: date.getMonth() + 1,
    day: date.getDate(),
    hour: date.getHours(),
  };
}

function daysInMonth(year: number, month: number) {
  return new Date(year, month, 0).getDate();
}

function serialize(value: PickerValue | null) {
  if (!value) return "";
  return `${value.year}-${pad(value.month)}-${pad(value.day)}T${pad(value.hour)}:00`;
}

function display(value: PickerValue | null) {
  if (!value) return "날짜와 시간 선택";
  const period = value.hour < 12 ? "오전" : "오후";
  const hour = value.hour % 12 || 12;
  return `${value.year}년 ${value.month}월 ${value.day}일 · ${period} ${hour}시`;
}

export default function DeadlineInput({ label = "투표 마감 시간", defaultValue }: { label?: string; defaultValue?: string }) {
  const initial = parseValue(defaultValue);
  const [value, setValue] = useState<PickerValue | null>(initial);
  const [draft, setDraft] = useState<PickerValue>(() => initialDraft(initial));
  const [open, setOpen] = useState(false);
  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 11 }, (_, index) => currentYear + index);
  const days = Array.from({ length: daysInMonth(draft.year, draft.month) }, (_, index) => index + 1);

  function update(part: keyof PickerValue, next: number) {
    setDraft((current) => {
      const updated = { ...current, [part]: next };
      if (part === "year" || part === "month") {
        updated.day = Math.min(updated.day, daysInMonth(updated.year, updated.month));
      }
      return updated;
    });
  }

  function openPicker() {
    setDraft(initialDraft(value));
    setOpen(true);
  }

  const twelveHour = draft.hour % 12 || 12;
  const period = draft.hour < 12 ? "AM" : "PM";

  function changeHour(hour: number) {
    update("hour", period === "AM" ? hour % 12 : (hour % 12) + 12);
  }

  function changePeriod(next: "AM" | "PM") {
    const hour = draft.hour % 12;
    update("hour", next === "AM" ? hour : hour + 12);
  }

  return (
    <Field label={label}>
      <div className="date-time-field">
        <input type="hidden" name="deadline" value={serialize(value)} />
        <button type="button" className={`date-time-trigger ${value ? "selected" : ""}`} aria-expanded={open} onClick={openPicker}>
          <span aria-hidden="true">◷</span>
          <span>{display(value)}</span>
          <b aria-hidden="true">⌄</b>
        </button>

        {open && (
          <div className="date-time-popover" role="dialog" aria-modal="false" aria-label="마감 날짜와 시간 선택">
            <div className="date-time-picker-heading">
              <strong>마감 날짜와 시간</strong>
              <small>각 항목을 스크롤해 선택하세요.</small>
            </div>

            <div className="picker-section-label">날짜</div>
            <div className="picker-wheels picker-date-wheels">
              <PickerColumn label="년" value={draft.year} options={years} render={(item) => `${item}년`} onChange={(next) => update("year", next)} />
              <PickerColumn label="월" value={draft.month} options={Array.from({ length: 12 }, (_, index) => index + 1)} render={(item) => `${item}월`} onChange={(next) => update("month", next)} />
              <PickerColumn label="일" value={draft.day} options={days} render={(item) => `${item}일`} onChange={(next) => update("day", next)} />
            </div>

            <div className="picker-section-label">시간</div>
            <div className="picker-wheels picker-time-wheels">
              <PickerColumn label="시" value={twelveHour} options={Array.from({ length: 12 }, (_, index) => index + 1)} render={(item) => pad(item)} onChange={changeHour} />
              <div className="picker-column period-column">
                <span>구분</span>
                <div className="period-options">
                  {(["AM", "PM"] as const).map((item) => <button type="button" className={period === item ? "active" : ""} key={item} onClick={() => changePeriod(item)}>{item === "AM" ? "오전" : "오후"}</button>)}
                </div>
              </div>
            </div>

            <div className="date-time-actions">
              <button type="button" className="button button-ghost" onClick={() => setOpen(false)}>취소</button>
              <button type="button" className="button button-primary" onClick={() => { setValue(draft); setOpen(false); }}>확인</button>
            </div>
          </div>
        )}
      </div>
    </Field>
  );
}

function PickerColumn({ label, value, options, render, onChange }: {
  label: string;
  value: number;
  options: number[];
  render: (value: number) => string;
  onChange: (value: number) => void;
}) {
  return <label className="picker-column"><span>{label}</span><select size={5} value={value} onChange={(event) => onChange(Number(event.target.value))}>{options.map((option) => <option value={option} key={option}>{render(option)}</option>)}</select></label>;
}
