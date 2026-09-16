package com.prgms.backend.global.exception.custom;

public class MeetingAccessDeniedException extends RuntimeException {
  public MeetingAccessDeniedException(String message) {
    super(message);
  }
}
