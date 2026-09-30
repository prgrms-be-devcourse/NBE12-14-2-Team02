package com.prgms.backend.global.exception;

/**
 * 우리가 의도적으로 던지는 모든 비즈니스 예외의 부모.
 * abstract라서 이 클래스를 직접 던질 수 없고, 반드시 구체적인 상황을 나타내는
 * 자식 클래스(ProductNotFoundException 등)를 만들어서 던져야 한다.
 * GlobalExceptionHandler가 이 타입 하나만 잡으면 모든 자식이 다 같이 처리된다.
 */
public abstract class BusinessException extends RuntimeException {
    private final int code;

    // RuntimeException(unchecked)을 상속해서, 메소드 시그니처에 throws를 안 붙여도
    // 어디서든 자유롭게 던질 수 있게 했다.
    protected BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
