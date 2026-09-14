package com.prgms.backend.global;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 응답을 감싸는 공통 포맷.
 * 성공/실패 응답을 {success, code, message, data} 구조로 표현한다.
 * 실제 HTTP 204 응답은 본문 없이 반환한다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private boolean success;
    private int code;
    private String message;
    private T data;

    // 가장 기본적인 성공 응답: code + 데이터
    public static <T> ApiResponse<T> success(int code, T data) {
        return new ApiResponse<>(true, code, "", data);
    }

    // 성공 응답 객체: 메시지와 데이터 없음
    public static ApiResponse<Void> noContentSuccess() {
        return new ApiResponse<>(true, 204, "", null);
    }

    // 성공 응답 객체: 데이터 없이 메시지만 포함
    public static ApiResponse<Void> noContentSuccess(String message) {
        return new ApiResponse<>(true, 204, message, null);
    }

    // 실패 + 데이터 있음
    // 예: 검증 실패한 필드 목록
    public static <T> ApiResponse<T> error(
            int code,
            String message,
            T data
    ) {
        return new ApiResponse<>(false, code, message, data);
    }

    // 실패 + 데이터 없음
    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}