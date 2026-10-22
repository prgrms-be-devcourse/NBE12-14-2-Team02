package com.prgms.backend.global.exception;

import com.prgms.backend.global.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 우리가 직접 정의해서 던진 예외(BusinessException과 그 자식들)를 처리.
    // 예외 안에 이미 담아둔 code/message를 그대로 응답에 실어보낸다.
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getCode())
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    // @Valid 검증 실패 시 Spring이 자동으로 던지는 예외.
    // 여러 필드가 동시에 실패할 수 있지만, 우선 첫 번째 에러 메시지만 내려준다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "잘못된 요청입니다.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, message));
    }

    // @RequestParam, @PathVariable 등 메서드 파라미터 검증 실패 시 Spring이 자동으로 던지는 예외.
    // 여러 검증 오류가 발생할 수 있지만, 우선 첫 번째 에러 메시지만 내려준다.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidationException(
            HandlerMethodValidationException e
    ) {
        String message = e.getAllErrors().get(0).getDefaultMessage();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, message));
    }

    // 위 두 핸들러에 안 걸리는 나머지 모든 예외(진짜 버그, DB 오류 등)를 잡는 최후 방어선.
    // 내부 예외 메시지를 그대로 노출하면 보안상 안 좋으므로 고정된 메시지만 내려준다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("예상하지 못한 예외 발생", e); // 응답엔 안 담고 서버 콘솔에만 스택트레이스 남김
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "서버 오류가 발생했습니다."));
    }

    // 요청 경로에 해당하는 API나 정적 리소스를 찾지 못하면 404로 응답
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        String message = e.getMessage();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, message));
    }

    // 필수 @RequestParam 파라미터가 누락되면 400으로 응답
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = e.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, message));
    }

    // 필수 요청 본문 누락 또는 JSON 파싱·역직렬화 실패 시 400으로 응답
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = e.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, message));
    }

    // 지원하지 않는 요청 Content-Type은 415로 응답
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .headers(e.getHeaders())
                .body(ApiResponse.error(415, "지원하지 않는 Content-Type입니다."));
    }

    // 지원하지 않는 HTTP 메서드는 405로 응답하고 Allow 헤더에 허용된 메서드를 전달
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(e.getHeaders())
                .body(ApiResponse.error(405, "지원하지 않는 HTTP 메서드입니다."));
    }

}
