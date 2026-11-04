package com.liaison.global.response;

import org.springframework.http.HttpStatus;

/**
 * 성공 응답의 공통 형식. 실패 응답은 RFC 9457 Problem Details 형식(GlobalExceptionHandler)으로 내려간다.
 *
 * @param code HTTP 상태 코드
 * @param status 성공 응답은 항상 "success"
 * @param message 사람이 읽는 설명
 * @param data 실제 데이터(없으면 null)
 */
public record ApiResponse<T>(int code, String status, String message, T data) {

    private static final String SUCCESS = "success";

    public static <T> ApiResponse<T> success(T data) {
        return of(HttpStatus.OK, data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return of(HttpStatus.CREATED, data);
    }

    public static <T> ApiResponse<T> accepted(T data) {
        return of(HttpStatus.ACCEPTED, data);
    }

    public static <T> ApiResponse<T> of(HttpStatus httpStatus, T data) {
        return new ApiResponse<>(httpStatus.value(), SUCCESS, httpStatus.getReasonPhrase(), data);
    }
}
