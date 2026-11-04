package com.liaison.global.error;

import org.springframework.http.HttpStatus;

/**
 * 오류 응답의 code 값. 앱·웹은 message 문구가 아니라 이 code로 분기한다.
 *
 * <p>message는 사용자에게 그대로 보여도 되는 문구만 쓴다. 학생 이름 같은 개인정보나 입력값을 넣지 않는다.
 */
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값을 확인해 주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없어요."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 정보를 찾을 수 없어요."),
    CONFLICT(HttpStatus.CONFLICT, "이미 처리됐거나 다른 변경과 겹쳤어요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
