package com.liaison.global.error;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 오류를 RFC 9457 Problem Details 형식으로 바꾼다. 추가 속성 code에 ErrorCode 이름을 넣는다.
 *
 * <p>개인정보 보호: 응답에 입력값(rejected value)이나 예외 메시지를 그대로 싣지 않는다. 서버 로그에도 요청 본문을 남기지 않는다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String CODE = "code";
    static final String ERRORS = "errors";

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.info("업무 예외: code={}, reason={}", errorCode, ex.getMessage());
        return ResponseEntity.status(errorCode.status()).body(problem(errorCode));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("처리하지 못한 예외", ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
                .body(problem(ErrorCode.INTERNAL_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail body = problem(ErrorCode.INVALID_REQUEST);
        List<FieldViolation> errors =
                ex.getBindingResult().getFieldErrors().stream().map(FieldViolation::from).toList();
        body.setProperty(ERRORS, errors);
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status()).body(body);
    }

    /** 스프링이 만든 나머지 표준 오류(404 경로 없음, 405 등)에도 code를 붙인다. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response =
                super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problemDetail) {
            problemDetail.setProperty(CODE, codeFor(statusCode).name());
        }
        return response;
    }

    private static ProblemDetail problem(ErrorCode errorCode) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(errorCode.status(), errorCode.message());
        detail.setProperty(CODE, errorCode.name());
        return detail;
    }

    private static ErrorCode codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> ErrorCode.INVALID_REQUEST;
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 409 -> ErrorCode.CONFLICT;
            default -> status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.INVALID_REQUEST;
        };
    }

    /** 입력 검증 실패 항목. 입력값 자체는 개인정보일 수 있어 싣지 않는다. */
    record FieldViolation(String field, String reason) {
        static FieldViolation from(FieldError error) {
            return new FieldViolation(error.getField(), error.getDefaultMessage());
        }
    }
}
