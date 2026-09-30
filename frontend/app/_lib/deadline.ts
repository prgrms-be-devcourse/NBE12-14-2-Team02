export function parseDeadline(value: FormDataEntryValue | null): string {
  const input = typeof value === "string" ? value.trim() : "";
  const match = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})$/.exec(input);
  if (!match) throw new Error("마감 날짜와 시간을 선택해주세요.");

  const [, yearText, monthText, dayText, hourText, minuteText] = match;
  const [year, month, day, hour, minute] = [yearText, monthText, dayText, hourText, minuteText].map(Number);
  const date = new Date(Date.UTC(year, month - 1, day, hour, minute));
  if (year < 1000 || date.getUTCFullYear() !== year || date.getUTCMonth() + 1 !== month ||
      date.getUTCDate() !== day || date.getUTCHours() !== hour || date.getUTCMinutes() !== minute) {
    throw new Error("존재하지 않는 날짜 또는 시간입니다. 다시 확인해주세요.");
  }

  return `${yearText}-${monthText}-${dayText}T${hourText}:${minuteText}:00`;
}

export function formatDeadlineInput(value: string): string {
  return value.slice(0, 16).replace(" ", "T");
}
