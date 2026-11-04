package com.liaison.global.error;

/** 업무 규칙 위반을 알리는 예외. 응답에는 ErrorCode의 문구만 나가고, 예외 메시지는 서버 로그에만 남는다. */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    public BusinessException(ErrorCode errorCode, String logMessage) {
        super(logMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
